# SmashMate — Design Specification

**Version:** 1.0  
**Date:** 2026-08-18  
**Status:** Draft — Pending Implementation  
**Scope:** MVP (Phase 1)

---

## 1. Overview

SmashMate là ứng dụng web quản lý câu lạc bộ cầu lông **single-tenant**, phục vụ 1 CLB cụ thể. Ứng dụng giải quyết 3 bài toán cốt lõi:

1. **Quản lý thành viên** — danh sách, hồ sơ, phân quyền
2. **Lịch sinh hoạt** — lịch định kỳ, buổi đột xuất, RSVP, checklist công việc
3. **Chia tiền** — chi phí sân, cầu (FIFO inventory), các hoạt động phụ (ăn sáng, v.v.)

### Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21 + Spring Boot 3.x (RESTful API, JPA/Hibernate, Spring Security, JWT) |
| Frontend | ReactJS + Vite + Tailwind CSS + Zustand + Axios |
| Database | MySQL 8.x |
| Migration | Flyway |

### Người dùng & Vai trò

| Role | Mô tả |
|------|-------|
| `ADMIN` | Quản lý CLB, kiêm thủ quỹ. Toàn quyền trên hệ thống. |
| `MEMBER` | Thành viên. Xem lịch, RSVP, xem chi phí & công nợ cá nhân. |

---

## 2. Module 1 — Quản lý Thành viên

### 2.1 Phân loại thành viên

| Role | Mô tả | Email/Password | Đăng nhập |
|------|---------|---------------|----------|
| `ADMIN` | Quản lý CLB, kiêm thủ quỹ | Bắt buộc | ✅ |
| `MEMBER` | Thành viên chính thức. Có thể có hoặc không có tài khoản. | Tùy chọn | ✅ Nếu có email+password |
| `GUEST` | Khách vãng lai / giao lưu. Admin tạo nhanh trong buổi. | Không có | ❌ Không bao giờ |

**Quy tắc:**
- `GUEST` luôn có `email = NULL` và `password = NULL` — backend enforce, không có ngoại lệ
- `MEMBER` có thể offline (NULL email/password) hoặc account (có email+password)
- `ADMIN` bắt buộc phải có email+password
- Admin có thể "nâng cấp" offline `MEMBER` lên account member bằng cách gán email+password sau

### 2.2 Tính năng

| Tính năng | ADMIN | MEMBER (account) | GUEST |
|-----------|-------|-----------------|-------|
| Xem danh sách thành viên | ✅ | ✅ | ❌ |
| Xem hồ sơ cá nhân | ✅ | Chỉ của mình | ❌ |
| Thêm / Chỉnh sửa / Xóa mềm thành viên | ✅ | ❌ | ❌ |
| Thêm GUEST nhanh cho buổi | ✅ | ❌ | ❌ |
| Gán email+password cho offline MEMBER | ✅ | ❌ | ❌ |
| Điểm danh / RSVP | ✅ | ✅ | ❌ (Admin điểm danh hộ) |
| Xem công nợ của mình | ✅ | ✅ | ❌ |
| Chia tiền buổi | Admin quản lý | — | — |

### 2.3 Data Model — `members`

```sql
CREATE TABLE members (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(100)    NOT NULL,
    phone       VARCHAR(20)     NULL,
    email       VARCHAR(150)    NULL,               -- NULL cho offline MEMBER và GUEST
    password    VARCHAR(255)    NULL,               -- NULL = không đăng nhập được
    role        ENUM('ADMIN','MEMBER','GUEST') NOT NULL DEFAULT 'MEMBER',
    status      ENUM('ACTIVE','INACTIVE','LEFT') NOT NULL DEFAULT 'ACTIVE',
    balance     DECIMAL(12, 2)  NOT NULL DEFAULT 0, -- dương = còn dư, âm = đang nợ
    joined_date DATE            NOT NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME        NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_members_email (email)             -- MySQL cho phép nhiều NULL trong UNIQUE KEY
);
```

> **Backend rules:**
> - `role = 'GUEST'` → `email` và `password` bắt buộc phải NULL
> - `role = 'ADMIN'` → `email` và `password` bắt buộc NOT NULL
> - `role = 'MEMBER'` → `email` và `password` có thể NULL hoặc NOT NULL (phải đi cặp)

### 2.4 API Endpoints

```
POST   /api/v1/auth/login                     → JWT token (chỉ ADMIN/MEMBER có email+password)
POST   /api/v1/auth/refresh                    → refresh access token
PATCH  /api/v1/auth/change-password

GET    /api/v1/members                         → danh sách thành viên (?role=MEMBER|GUEST)
POST   /api/v1/members                         → thêm MEMBER (tối thiểu: full_name) [ADMIN]
POST   /api/v1/members/guests                  → tạo GUEST nhanh (chỉ cần full_name) [ADMIN]
GET    /api/v1/members/{id}                    → chi tiết [ADMIN | chủ tài khoản]
PUT    /api/v1/members/{id}                    → cập nhật [ADMIN]
PATCH  /api/v1/members/{id}/account           → gán email + mật khẩu cho offline MEMBER [ADMIN]
PATCH  /api/v1/members/{id}/status             → kích hoạt / vô hiệu hóa [ADMIN]
DELETE /api/v1/members/{id}                    → soft delete [ADMIN]
```

---

## 3. Module 2 — Lịch Sinh hoạt

### 3.1 Tổng quan

Hệ thống có 2 loại lịch:

- **Recurring Schedule**: Lịch tập định kỳ hàng tuần (ví dụ: thứ 3, thứ 5, 6:00–8:00)
- **Session**: Buổi sinh hoạt cụ thể — có thể sinh từ lịch định kỳ hoặc tạo đột xuất

### 3.2 Recurring Schedule

#### Data Model — `recurring_schedules`

```sql
CREATE TABLE recurring_schedules (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    day_of_week     TINYINT     NOT NULL,   -- 1=Monday ... 7=Sunday (ISO)
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    venue_name      VARCHAR(200) NULL,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
```

#### API Endpoints

```
GET    /api/v1/schedules           → danh sách lịch định kỳ
POST   /api/v1/schedules           → tạo lịch định kỳ [ADMIN]
PUT    /api/v1/schedules/{id}      → cập nhật [ADMIN]
PATCH  /api/v1/schedules/{id}/toggle → bật/tắt [ADMIN]
DELETE /api/v1/schedules/{id}      → xóa [ADMIN]
```

### 3.3 Session (Buổi sinh hoạt)

#### Trạng thái Session

```
UPCOMING → IN_PROGRESS → CLOSED
     ↓
 CANCELLED
```

#### Data Model — `sessions`

```sql
CREATE TABLE sessions (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    schedule_id     BIGINT          NULL,              -- NULL nếu đột xuất
    session_date    DATE            NOT NULL,
    start_time      TIME            NOT NULL,
    end_time        TIME            NULL,
    venue_name      VARCHAR(200)    NULL,
    status          ENUM('UPCOMING','IN_PROGRESS','CLOSED','CANCELLED')
                                    NOT NULL DEFAULT 'UPCOMING',
    notes           TEXT            NULL,
    created_by      BIGINT          NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_sessions_date (session_date),
    INDEX idx_sessions_status (status),
    CONSTRAINT fk_sessions_schedule FOREIGN KEY (schedule_id) REFERENCES recurring_schedules(id),
    CONSTRAINT fk_sessions_created_by FOREIGN KEY (created_by) REFERENCES members(id)
);
```

#### Data Model — `session_attendees`

```sql
CREATE TABLE session_attendees (
    id              BIGINT  NOT NULL AUTO_INCREMENT,
    session_id      BIGINT  NOT NULL,
    member_id       BIGINT  NOT NULL,
    rsvp_status     ENUM('ATTENDING','ABSENT','PENDING') NOT NULL DEFAULT 'PENDING',
    checked_in      BOOLEAN NOT NULL DEFAULT FALSE,   -- điểm danh thực tế
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_session_attendees (session_id, member_id),
    CONSTRAINT fk_sa_session FOREIGN KEY (session_id) REFERENCES sessions(id),
    CONSTRAINT fk_sa_member  FOREIGN KEY (member_id)  REFERENCES members(id)
);
```

### 3.4 Session Task Checklist

Mỗi buổi có danh sách công việc cần làm (đặt sân, mua cầu, mua nước...).

#### Data Model — `session_tasks`

```sql
CREATE TABLE session_tasks (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    session_id      BIGINT          NOT NULL,
    title           VARCHAR(200)    NOT NULL,   -- "Đặt sân", "Mua cầu"...
    assigned_to     BIGINT          NULL,       -- member_id
    is_done         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_session_tasks_session (session_id),
    CONSTRAINT fk_tasks_session FOREIGN KEY (session_id) REFERENCES sessions(id),
    CONSTRAINT fk_tasks_member  FOREIGN KEY (assigned_to) REFERENCES members(id)
);
```

#### API Endpoints

```
GET    /api/v1/sessions                    → danh sách (có filter theo status, tháng)
POST   /api/v1/sessions                    → tạo buổi [ADMIN]
GET    /api/v1/sessions/{id}               → chi tiết buổi
PUT    /api/v1/sessions/{id}               → cập nhật [ADMIN]
PATCH  /api/v1/sessions/{id}/status        → đổi status [ADMIN]
DELETE /api/v1/sessions/{id}               → hủy/xóa [ADMIN]

GET    /api/v1/sessions/{id}/attendees     → danh sách điểm danh
PATCH  /api/v1/sessions/{id}/rsvp          → member tự RSVP
PATCH  /api/v1/sessions/{id}/attendees/{memberId}/checkin  → điểm danh thực tế [ADMIN]

GET    /api/v1/sessions/{id}/tasks         → danh sách task checklist
POST   /api/v1/sessions/{id}/tasks         → tạo task [ADMIN]
PATCH  /api/v1/sessions/{id}/tasks/{taskId} → cập nhật / đánh dấu done [ADMIN]
DELETE /api/v1/sessions/{id}/tasks/{taskId} → xóa task [ADMIN]
```

---

## 4. Module 3 — Quản lý Cầu (Shuttle Inventory)

Đây là module phức tạp nhất. Hệ thống theo dõi tồn kho cầu theo từng lô và áp dụng **FIFO** khi tính chi phí cầu mỗi buổi.

### 4.1 Luồng nghiệp vụ

```
1. Thành viên A mua 1 tuýp cầu (12 quả, 325.000đ)
   → Admin tạo ShuttleBatch: quantity=12, remaining=12, unit_price=27.083đ

2. Buổi kết thúc, Admin nhập "dùng 10 quả"
   → Hệ thống FIFO: lấy 10 quả từ Batch A (đang là batch cũ nhất còn cầu)
   → Tạo SessionShuttleUsage: batch=A, quantity_used=10, cost=270.830đ
   → Cập nhật Batch A: remaining = 2

3. Admin nhập thêm "dùng 1 quả nữa" (override thủ công từ Batch B)
   → Tạo SessionShuttleUsage: batch=B, quantity_used=1, cost=27.500đ

4. Tổng chi phí cầu buổi đó = Σ cost của các SessionShuttleUsage
```

### 4.2 Data Model

#### `shuttle_batches`

```sql
CREATE TABLE shuttle_batches (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    purchased_by    BIGINT          NOT NULL,       -- member_id người mua
    quantity        INT             NOT NULL,       -- tổng số quả trong tuýp
    remaining       INT             NOT NULL,       -- còn lại (giảm dần theo FIFO)
    unit_price      DECIMAL(12, 2)  NOT NULL,       -- giá mỗi quả = tổng / số quả
    total_price     DECIMAL(12, 2)  NOT NULL,       -- tổng tiền tuýp (do người mua ứng)
    purchase_date   DATE            NOT NULL,
    notes           VARCHAR(500)    NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_shuttle_batches_remaining (remaining),
    CONSTRAINT fk_batches_member FOREIGN KEY (purchased_by) REFERENCES members(id)
);
```

#### `session_shuttle_usages`

```sql
CREATE TABLE session_shuttle_usages (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    session_id      BIGINT          NOT NULL,
    batch_id        BIGINT          NOT NULL,
    quantity_used   INT             NOT NULL,
    unit_price      DECIMAL(12, 2)  NOT NULL,       -- snapshot giá lúc dùng
    total_cost      DECIMAL(12, 2)  NOT NULL,       -- quantity_used × unit_price
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_ssu_session (session_id),
    CONSTRAINT fk_ssu_session FOREIGN KEY (session_id) REFERENCES sessions(id),
    CONSTRAINT fk_ssu_batch   FOREIGN KEY (batch_id)   REFERENCES shuttle_batches(id)
);
```

### 4.3 FIFO Algorithm

Khi Admin nhập `quantityUsed` cho một session:

```
1. Lấy danh sách shuttle_batches WHERE remaining > 0 ORDER BY purchase_date ASC, id ASC
2. Duyệt qua từng batch theo thứ tự:
   a. Lấy min(remaining, quantityNeeded) từ batch hiện tại
   b. Tạo SessionShuttleUsage record
   c. Trừ remaining của batch
   d. Giảm quantityNeeded
   e. Nếu quantityNeeded = 0 → dừng
3. Nếu quantityNeeded > 0 sau khi duyệt hết → báo lỗi "Không đủ cầu trong kho"
```

**Override thủ công:** Admin có thể tự chỉ định lấy từ batch nào, bao nhiêu quả → bỏ qua FIFO, hệ thống tạo trực tiếp `SessionShuttleUsage` theo chỉ định.

### 4.4 API Endpoints

```
GET    /api/v1/shuttle/batches              → danh sách lô cầu (kể cả đã hết)
POST   /api/v1/shuttle/batches              → nhập lô cầu mới [ADMIN]
GET    /api/v1/shuttle/batches/inventory    → chỉ lô còn cầu (tồn kho hiện tại)

POST   /api/v1/sessions/{id}/shuttle/usage          → nhập số quả đã dùng (FIFO) [ADMIN]
POST   /api/v1/sessions/{id}/shuttle/usage/manual   → override thủ công từng batch [ADMIN]
GET    /api/v1/sessions/{id}/shuttle/usage           → xem usage của buổi
DELETE /api/v1/sessions/{id}/shuttle/usage           → xóa, rollback remaining [ADMIN]
```

---

## 5. Module 4 — Quản lý Chi phí & Chia tiền

### 5.1 Tổng quan

Mỗi buổi sinh hoạt có nhiều **khoản chi** (`expense_items`). Mỗi khoản chi có danh sách **người tham gia riêng** (`expense_participants`) — không phải tất cả người điểm danh đều chia mọi khoản.

**Ví dụ buổi 20/8:**
- Khoản 1: Tiền sân 300.000đ → 9 người chơi → mỗi người 33.333đ
- Khoản 2: Tiền cầu 246.666đ → 9 người chơi → mỗi người 27.407đ  
- Khoản 3: Ăn sáng 400.000đ → 8 người đi ăn → mỗi người 50.000đ

> **Lưu ý:** Tiền cầu được tính tự động từ Module 3. Admin không nhập thủ công tiền cầu.

### 5.2 Danh mục khoản chi (`expense_categories`)

Thay vì dùng enum cứng, loại khoản chi được quản lý qua bảng riêng để:
- Admin tùy chỉnh thêm/sửa danh mục
- Thống kê chi tiêu theo danh mục trong Phase 2

**System categories** (seeded sẵn, không được xóa):

| code | name | is_system | Ghi chú |
|------|------|-----------|---------|
| `COURT_FEE` | Tiền sân | `true` | Auto-link khi close session |
| `SHUTTLE_FEE` | Tiền cầu | `true` | Auto-generated từ FIFO usage |
| `FOOD` | Ăn uống | `false` | Mặc định có sẵn, có thể xóa |
| `OTHER` | Khác | `false` | Mặc định có sẵn, có thể xóa |

#### Data Model — `expense_categories`

```sql
CREATE TABLE expense_categories (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)     NOT NULL,   -- định danh nội bộ (dùng cho logic hệ thống)
    name        VARCHAR(100)    NOT NULL,   -- hiển thị: "Tiền sân", "Ăn uống"
    description VARCHAR(300)    NULL,
    is_system   BOOLEAN         NOT NULL DEFAULT FALSE,  -- TRUE = không được xóa
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order  INT             NOT NULL DEFAULT 0,      -- thứ tự hiển thị
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_expense_categories_code (code)
);
```

#### API Endpoints — `expense_categories`

```
GET    /api/v1/expense-categories         → danh sách danh mục (active)
POST   /api/v1/expense-categories         → tạo danh mục [ADMIN]
PUT    /api/v1/expense-categories/{id}    → cập nhật tên/mô tả [ADMIN]
DELETE /api/v1/expense-categories/{id}    → xóa [ADMIN] (chặn nếu is_system=true)
```

> **Rule:** Danh mục có `is_system=true` không thể bị xóa hoặc đổi `code`. Chỉ được đổi `name` và `description`.

### 5.3 Data Model

#### `expense_items`

```sql
CREATE TABLE expense_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    session_id      BIGINT          NOT NULL,
    category_id     BIGINT          NOT NULL,           -- FK → expense_categories
    description     VARCHAR(300)    NOT NULL,           -- "Tiền sân", "Ăn sáng Phở 24"
    total_amount    DECIMAL(12, 2)  NOT NULL,
    paid_by         BIGINT          NULL,               -- ai đã ứng tiền (NULL = quỹ CLB)
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_expense_items_session (session_id),
    INDEX idx_expense_items_category (category_id),
    CONSTRAINT fk_ei_session   FOREIGN KEY (session_id)  REFERENCES sessions(id),
    CONSTRAINT fk_ei_category  FOREIGN KEY (category_id) REFERENCES expense_categories(id),
    CONSTRAINT fk_ei_paid_by   FOREIGN KEY (paid_by)     REFERENCES members(id)
);
```

#### `expense_participants`

```sql
CREATE TABLE expense_participants (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    expense_item_id BIGINT          NOT NULL,
    member_id       BIGINT          NOT NULL,
    share_amount    DECIMAL(12, 2)  NOT NULL,       -- phần tiền người này phải trả
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_expense_participant (expense_item_id, member_id),
    CONSTRAINT fk_ep_expense FOREIGN KEY (expense_item_id) REFERENCES expense_items(id),
    CONSTRAINT fk_ep_member  FOREIGN KEY (member_id)       REFERENCES members(id)
);
```

### 5.4 Công thức chia tiền

```
share_amount = FLOOR(total_amount / participant_count)
```

Phần dư (do làm tròn) cộng vào phần của người đầu tiên trong danh sách (thường là người đã ứng tiền).

### 5.5 Luồng kết buổi (Close Session)

Khi Admin bấm "Kết buổi":

```
1. Tính tổng chi phí cầu từ session_shuttle_usages
   → Tự động tạo expense_item với category code = 'SHUTTLE_FEE'
2. Admin xác nhận/chỉnh sửa danh sách "ai chơi cầu" → tạo expense_participants
3. Admin nhập tiền sân → tạo expense_item với category code = 'COURT_FEE' + expense_participants
4. Admin nhập các khoản phụ (chọn từ expense_categories) + chọn ai tham gia
   → tạo expense_item + expense_participants
5. Hệ thống tính share_amount cho từng người từng khoản
6. Tổng hợp: mỗi thành viên có số tiền phải trả = Σ share_amount - số tiền đã ứng
7. Tạo payment_debts record cho từng người còn nợ
8. Session chuyển status → CLOSED
```

### 5.6 API Endpoints

```
GET    /api/v1/sessions/{id}/expenses               → danh sách khoản chi
POST   /api/v1/sessions/{id}/expenses               → tạo khoản chi [ADMIN]
PUT    /api/v1/sessions/{id}/expenses/{expenseId}   → cập nhật [ADMIN]
DELETE /api/v1/sessions/{id}/expenses/{expenseId}   → xóa [ADMIN]

POST   /api/v1/sessions/{id}/close                  → kết buổi, tính tiền [ADMIN]
GET    /api/v1/sessions/{id}/summary                → tóm tắt chi phí buổi
```

---

## 6. Module 5 — Theo dõi Thanh toán

### 6.1 Tổng quan

Mỗi thành viên có một **balance** (số dư chạy):
- **Dương (+)**: thành viên đang có tiền thừa trong CLB (đã trả dư trước đó)
- **Âm (−)**: thành viên đang nợ CLB

Trên màn hình công nợ, hệ thống hiển thị kèm **QR code ngân hàng** của CLB (nếu Admin đã cấu hình) để thành viên quét chuyển khoản.

### 6.2 Luồng tính nợ khi Close Session

```
Với mỗi thành viên tham gia buổi:
  gross_owed = Σ share_amount của thành viên đó (từ expense_participants)

  Nếu balance >= gross_owed:
    → Không tạo payment_debts (đã cấn trừ hết)
    → balance -= gross_owed
    → Ghi member_balance_log: amount = -gross_owed, reason = "Cấn trừ buổi {date}"
  Nếu balance < gross_owed:
    → net_owed = gross_owed - balance
    → Tạo payment_debts với amount_owed = net_owed
    → balance = 0
    → Ghi member_balance_log nếu balance > 0 trước đó
```

### 6.3 Luồng xác nhận thanh toán

```
Admin nhập amount_paid (số tiền thực tế thành viên chuyển):

  Nếu amount_paid >= amount_owed:
    → is_settled = true, settled_at = now()
    → overpaid = amount_paid - amount_owed
    → balance += overpaid
    → Ghi member_balance_log: amount = +overpaid, reason = "Trả dư buổi {date}"
  Nếu amount_paid < amount_owed:
    → amount_paid cập nhật, is_settled = false (vẫn còn nợ)
```

### 6.4 Data Model

#### `payment_debts`

```sql
CREATE TABLE payment_debts (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    session_id      BIGINT          NOT NULL,
    member_id       BIGINT          NOT NULL,
    gross_owed      DECIMAL(12, 2)  NOT NULL,  -- tổng chi phí trước khi cấn trừ balance
    balance_used    DECIMAL(12, 2)  NOT NULL DEFAULT 0, -- số tiền balance đã cấn trừ
    amount_owed     DECIMAL(12, 2)  NOT NULL,  -- = gross_owed - balance_used (số phải trả thực)
    amount_paid     DECIMAL(12, 2)  NOT NULL DEFAULT 0, -- số tiền thực tế đã trả
    is_settled      BOOLEAN         NOT NULL DEFAULT FALSE,
    settled_at      DATETIME        NULL,
    confirmed_by    BIGINT          NULL,      -- admin xác nhận
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_debt_session_member (session_id, member_id),
    INDEX idx_debts_member (member_id),
    CONSTRAINT fk_debts_session   FOREIGN KEY (session_id)   REFERENCES sessions(id),
    CONSTRAINT fk_debts_member    FOREIGN KEY (member_id)    REFERENCES members(id),
    CONSTRAINT fk_debts_confirmed FOREIGN KEY (confirmed_by) REFERENCES members(id)
);
```

#### `member_balance_logs`

Audit trail cho mọi thay đổi balance — không bao giờ xóa.

```sql
CREATE TABLE member_balance_logs (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    member_id   BIGINT          NOT NULL,
    amount      DECIMAL(12, 2)  NOT NULL,       -- dương = credit, âm = debit
    balance_after DECIMAL(12, 2) NOT NULL,      -- balance của member sau thay đổi này
    reason      VARCHAR(300)    NOT NULL,       -- "Trả dư buổi 20/8", "Cấn trừ buổi 25/8"
    ref_type    ENUM('DEBT_SETTLE','BALANCE_DEDUCT','MANUAL') NOT NULL,
    ref_id      BIGINT          NULL,           -- payment_debts.id hoặc NULL
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_balance_logs_member (member_id),
    CONSTRAINT fk_bl_member FOREIGN KEY (member_id) REFERENCES members(id)
);
```

### 6.5 API Endpoints

```
GET    /api/v1/debts                           → tổng nợ toàn CLB [ADMIN]
GET    /api/v1/debts/me                        → nợ + balance của bản thân [MEMBER]
GET    /api/v1/debts/member/{memberId}         → nợ + balance của 1 thành viên [ADMIN]
PATCH  /api/v1/debts/{id}/settle               → xác nhận đã trả (nhập amount_paid) [ADMIN]
GET    /api/v1/sessions/{id}/debts             → nợ của 1 buổi cụ thể

GET    /api/v1/members/{id}/balance-logs       → lịch sử balance [ADMIN | chủ tài khoản]
```

---

## 7. Module 6 — Cài đặt CLB (Club Settings)

### 7.1 Tổng quan

Lưu các cấu hình chung của CLB dưới dạng key-value. MVP chỉ cần lưu QR code ngân hàng, nhưng định hướng có thể mở rộng sau (tên CLB, logo, giờ mở cửa sân mặc định...).

### 7.2 Data Model — `club_settings`

```sql
CREATE TABLE club_settings (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    setting_key     VARCHAR(100)    NOT NULL,   -- ví dụ: 'payment_qr_image_path', 'club_name'
    setting_value   TEXT            NULL,       -- giá trị (string, path, số, JSON...)
    description     VARCHAR(300)    NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_club_settings_key (setting_key)
);
```

**Seeded key cho MVP:**

| setting_key | Mô tả | Giá trị mặc định |
|-------------|---------|------------------|
| `payment_qr_image_path` | Đường dẫn file ảnh QR code ngân hàng | `null` |
| `club_name` | Tên CLB hiển thị | `"SmashMate"` |

### 7.3 QR Code Upload

- Admin tải ảnh QR code lên server (JPEG/PNG, tối đa 2MB)
- File được lưu trong thư mục `uploads/qr/` trên server (ngoài webroot)
- Đường dẫn file được lưu vào `club_settings` với key `payment_qr_image_path`
- Frontend hiển thị ảnh qua endpoint: `GET /api/v1/settings/payment-qr/image` (backend đọc file và trả về binary)

### 7.4 API Endpoints

```
GET    /api/v1/settings                          → lấy toàn bộ cài đặt [ADMIN]
GET    /api/v1/settings/payment-qr               → lấy metadata + URL ảnh QR (public)
GET    /api/v1/settings/payment-qr/image         → trả về binary ảnh QR (public)
PUT    /api/v1/settings                          → cập nhật cài đặt [ADMIN]
POST   /api/v1/settings/payment-qr/upload        → tải ảnh QR lên [ADMIN] (multipart/form-data)
DELETE /api/v1/settings/payment-qr               → xóa ảnh QR [ADMIN]
```

> `GET /api/v1/settings/payment-qr` được mở public (không cần ADMIN role) để MEMBER xem được QR code khi trả tiền.

---

## 8. Frontend Architecture

### 8.1 Routing Structure

```
/login                     → Trang đăng nhập

/dashboard                 → Tổng quan (upcoming sessions, quick stats)

/members                   → Danh sách thành viên [ADMIN]
/members/:id               → Hồ sơ thành viên

/schedules                 → Lịch tập định kỳ [ADMIN]

/sessions                  → Danh sách buổi sinh hoạt
/sessions/:id              → Chi tiết buổi (attendance, tasks, expenses)
/sessions/:id/close        → Luồng kết buổi [ADMIN]

/shuttle                   → Kho cầu & lịch sử [ADMIN]

/debts                     → Công nợ toàn CLB [ADMIN]
/debts/me                  → Công nợ của tôi [MEMBER]

/settings                  → Cài đặt CLB [ADMIN] (tên CLB, QR code ngân hàng)
```

### 8.2 State Management

```
useAuthStore       → user info, token, login/logout
useSessionStore    → danh sách sessions, session detail
useMemberStore     → danh sách members
useShuttleStore    → shuttle batches, inventory
useExpenseStore    → expense items, debts
useSettingsStore   → club settings, payment QR URL
```

### 8.3 Key UI Components

```
SessionCard           → Thẻ buổi sinh hoạt (status badge, date, venue)
AttendanceChecklist   → Điểm danh thành viên
TaskChecklist         → Checklist công việc
ShuttleUsageForm      → Nhập số cầu đã dùng (FIFO auto / manual override)
ExpenseSplitTable     → Bảng chia tiền từng khoản
DebtSummary           → Tóm tắt công nợ buổi
PaymentQRCode         → Hiển thị QR code ngân hàng trên màn hình nợ
```

---

## 9. Security

- JWT Access Token: 15 phút (ngắn để bảo mật)
- JWT Refresh Token: 7 ngày, lưu trong HttpOnly cookie
- Password: BCrypt hash, độ phức tạp factor 12
- CORS: chỉ cho phép origin của frontend
- Mọi endpoint (trừ `/auth/login` và `GET /api/v1/settings/payment-qr`) yêu cầu JWT hợp lệ
- Phân quyền bằng Spring Security `@PreAuthorize`
- File upload: chỉ chấp nhận JPEG/PNG, giới hạn 2MB, lưu ngoài webroot

---

## 10. MVP Scope (Phase 1)

### Bao gồm ✅

- Authentication (login, refresh token, đổi mật khẩu)
- CRUD Members
- CRUD Recurring Schedules
- CRUD Sessions (đột xuất & từ lịch định kỳ)
- RSVP & Check-in
- Session Task Checklist
- Shuttle Batch nhập kho
- Shuttle FIFO usage + manual override
- CRUD Expense Categories (kèm system categories seeded sẵn)
- Expense items (court fee, shuttle fee tự động, food, other)
- Expense participants (chia tiền theo nhóm tham gia)
- Close Session flow
- Payment Debts tracking
- Admin xác nhận thanh toán
- Club Settings: tải & hiển thị QR code ngân hàng

### Không bao gồm (Phase 2) ❌

- Quỹ CLB (club fund) — thu/chi tổng
- Thông báo/nhắc lịch (email, push notification)
- Báo cáo thống kê nâng cao (chi tiêu theo danh mục theo tháng)
- Ảnh đại diện thành viên
- Export PDF/Excel
