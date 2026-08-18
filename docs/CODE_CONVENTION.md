# SmashMate — Code Convention

> Tài liệu này là chuẩn chung bắt buộc áp dụng cho toàn bộ dự án SmashMate.
> Mọi thành viên phát triển phải tuân thủ trước khi tạo PR/merge code.

---

## 1. General Rules (Áp dụng cho cả dự án)

- Ngôn ngữ viết code, comment, tên biến, tên hàm: **tiếng Anh**
- Ngôn ngữ commit message: **tiếng Anh**, theo [Conventional Commits](https://www.conventionalcommits.org/)
- Không commit code thừa: `console.log`, `System.out.println`, code comment-out không cần thiết
- Mỗi file/class chỉ làm **một việc** (Single Responsibility)
- Không dùng magic number/string — dùng **constant hoặc enum**

### Commit Message Format

```
<type>(<scope>): <short description>

Types: feat | fix | docs | style | refactor | test | chore
Scope: auth | member | session | expense | shuttle | report

Examples:
  feat(session): add RSVP endpoint
  fix(expense): correct FIFO shuttle cost calculation
  docs(convention): add database naming rules
```

---

## 2. Backend — Java + Spring Boot

### 2.1 Naming Conventions

| Thành phần | Convention | Ví dụ |
|-----------|------------|-------|
| Class | `PascalCase` | `SessionService`, `ExpenseController` |
| Interface | `PascalCase` | `ShuttleRepository` |
| Method | `camelCase` | `calculateShuttleCost()` |
| Variable | `camelCase` | `totalAmount`, `shuttleBatch` |
| Constant | `SCREAMING_SNAKE_CASE` | `MAX_BATCH_SIZE`, `DEFAULT_PAGE_SIZE` |
| Package | `lowercase` | `com.smashmate.session` |
| Enum | `PascalCase`, values `SCREAMING_SNAKE_CASE` | `SessionStatus.IN_PROGRESS` |

### 2.2 Package Structure

```
com.smashmate
├── config/           # Spring config, Security config, CORS
├── controller/       # REST controllers (thin — chỉ gọi service)
├── service/          # Business logic
│   └── impl/         # Service implementations
├── repository/       # JPA Repositories
├── entity/           # JPA Entities (map 1-1 với bảng DB)
├── dto/
│   ├── request/      # Request body DTOs
│   └── response/     # Response body DTOs
├── mapper/           # Entity ↔ DTO mapping (MapStruct)
├── exception/        # Custom exceptions + GlobalExceptionHandler
├── security/         # JWT filter, UserDetails, SecurityConfig
└── util/             # Utility classes (DateUtil, CostCalculator...)
```

### 2.3 REST API Conventions

- Base URL: `/api/v1`
- Noun, số nhiều: `/api/v1/sessions`, `/api/v1/members`
- Dùng HTTP verbs đúng mục đích:

| Action | Method | URL |
|--------|--------|-----|
| Lấy danh sách | `GET` | `/api/v1/sessions` |
| Lấy chi tiết | `GET` | `/api/v1/sessions/{id}` |
| Tạo mới | `POST` | `/api/v1/sessions` |
| Cập nhật toàn bộ | `PUT` | `/api/v1/sessions/{id}` |
| Cập nhật một phần | `PATCH` | `/api/v1/sessions/{id}/status` |
| Xóa | `DELETE` | `/api/v1/sessions/{id}` |
| Action phức tạp | `POST` | `/api/v1/sessions/{id}/close` |

- Response luôn wrap trong envelope:

```json
{
  "success": true,
  "data": { ... },
  "message": "Session created successfully",
  "timestamp": "2026-08-18T10:00:00Z"
}
```

- Lỗi trả về chuẩn:

```json
{
  "success": false,
  "error": "SHUTTLE_BATCH_NOT_FOUND",
  "message": "Shuttle batch with id 5 not found",
  "timestamp": "2026-08-18T10:00:00Z"
}
```

### 2.4 Entity Rules

```java
@Entity
@Table(name = "shuttle_batches")          // snake_case, số nhiều
@Getter @Setter                           // Dùng Lombok
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShuttleBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;             // camelCase

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;         // Dùng BigDecimal cho tiền — KHÔNG dùng double/float

    @ManyToOne(fetch = FetchType.LAZY)    // Lazy by default
    @JoinColumn(name = "purchased_by_member_id")
    private Member purchasedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**Bắt buộc:**
- Tiền tệ: luôn dùng `BigDecimal`, không dùng `double`/`float`
- Thời gian: dùng `LocalDateTime` (UTC), không dùng `Date`
- Không dùng `@Data` của Lombok trên Entity (gây vấn đề với `equals/hashCode` và lazy loading)
- `fetch = FetchType.LAZY` cho mọi quan hệ — chỉ `EAGER` khi có lý do rõ ràng

### 2.5 Service Layer Rules

```java
@Service
@RequiredArgsConstructor
@Transactional                            // Mặc định transactional ở service
public class ShuttleService {

    private final ShuttleRepository shuttleRepository;
    private final SessionRepository sessionRepository;

    @Transactional(readOnly = true)       // Query-only methods đánh dấu readOnly
    public List<ShuttleBatchResponse> getBatches() { ... }

    public ShuttleCostResult calculateFifoCost(Long sessionId, Integer quantity) {
        // Business logic ở đây, không ở controller
    }
}
```

### 2.6 Exception Handling

```java
// Custom exception
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }
}

// Global handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(ResourceNotFoundException ex) { ... }
}
```

---

## 3. Frontend — ReactJS + Vite + Tailwind CSS

### 3.1 Naming Conventions

| Thành phần | Convention | Ví dụ |
|-----------|------------|-------|
| Component file | `PascalCase.tsx` | `SessionCard.tsx`, `ShuttleBatchForm.tsx` |
| Hook file | `camelCase.ts`, prefix `use` | `useSession.ts`, `useShuttleBatch.ts` |
| Utility file | `camelCase.ts` | `formatCurrency.ts`, `fifoCalculator.ts` |
| Type/Interface | `PascalCase` | `SessionResponse`, `ExpenseRequest` |
| Constant | `SCREAMING_SNAKE_CASE` | `API_BASE_URL`, `DEFAULT_PAGE_SIZE` |
| Zustand store | `use<Feature>Store` | `useSessionStore`, `useMemberStore` |

### 3.2 Folder Structure

```
src/
├── api/              # Axios instance + API calls theo feature
│   ├── axios.ts      # Axios config, interceptors, token refresh
│   ├── sessions.ts
│   ├── expenses.ts
│   └── shuttle.ts
├── components/
│   ├── common/       # Shared: Button, Modal, Badge, Table, Input...
│   └── <feature>/    # Feature-specific components
│       ├── session/
│       │   ├── SessionCard.tsx
│       │   └── SessionForm.tsx
│       └── expense/
│           └── ExpenseSplitTable.tsx
├── pages/            # Route-level pages
├── hooks/            # Custom hooks
├── stores/           # Zustand stores
├── types/            # TypeScript types/interfaces
├── utils/            # Pure utility functions
├── constants/        # App-wide constants
└── assets/           # Images, icons
```

### 3.3 Component Rules

```tsx
// ✅ Đúng — named export, props typed rõ ràng
interface SessionCardProps {
  session: SessionResponse;
  onClose?: (id: number) => void;
}

export function SessionCard({ session, onClose }: SessionCardProps) {
  return (
    <div className="rounded-xl bg-white shadow-sm p-4">
      ...
    </div>
  );
}

// ❌ Sai — default export ẩn, props không typed
export default function Card(props: any) { ... }
```

**Quy tắc:**
- Luôn dùng **named export** (không dùng `export default`)
- Props phải có **TypeScript interface** riêng, đặt tên `<ComponentName>Props`
- Không dùng `any` — thay bằng `unknown` hoặc type cụ thể
- Tách **logic** (hooks) khỏi **UI** (JSX trong component)

### 3.4 API Layer

```ts
// src/api/sessions.ts
import { api } from './axios';
import type { SessionRequest, SessionResponse, ApiResponse } from '@/types';

export const sessionApi = {
  getAll: () =>
    api.get<ApiResponse<SessionResponse[]>>('/sessions'),

  getById: (id: number) =>
    api.get<ApiResponse<SessionResponse>>(`/sessions/${id}`),

  create: (data: SessionRequest) =>
    api.post<ApiResponse<SessionResponse>>('/sessions', data),

  close: (id: number) =>
    api.post<ApiResponse<void>>(`/sessions/${id}/close`),
};
```

### 3.5 State Management (Zustand)

```ts
// src/stores/useSessionStore.ts
interface SessionStore {
  sessions: SessionResponse[];
  isLoading: boolean;
  fetchSessions: () => Promise<void>;
}

export const useSessionStore = create<SessionStore>((set) => ({
  sessions: [],
  isLoading: false,
  fetchSessions: async () => {
    set({ isLoading: true });
    const { data } = await sessionApi.getAll();
    set({ sessions: data.data, isLoading: false });
  },
}));
```

### 3.6 Tiền tệ & Số

```ts
// src/utils/formatCurrency.ts
export function formatVND(amount: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount);
}
// Kết quả: "325.000 ₫"
```

> ⚠️ **Không tính toán số tiền ở frontend** — mọi logic tính tiền do backend xử lý.
> Frontend chỉ **hiển thị** kết quả trả về từ API.

---

## 4. Database — MySQL

### 4.1 Naming Conventions

| Thành phần | Convention | Ví dụ |
|-----------|------------|-------|
| Table name | `snake_case`, **số nhiều** | `sessions`, `shuttle_batches`, `expense_items` |
| Column name | `snake_case` | `unit_price`, `purchased_by_member_id` |
| Primary key | `id` (BIGINT, AUTO_INCREMENT) | `id` |
| Foreign key column | `<table_singular>_id` | `session_id`, `member_id` |
| Index | `idx_<table>_<column(s)>` | `idx_sessions_date` |
| Unique constraint | `uq_<table>_<column(s)>` | `uq_members_email` |
| FK constraint | `fk_<table>_<ref_table>` | `fk_shuttle_batches_session` |
| Join table | `<table1_singular>_<table2_singular>` | `expense_members` |

### 4.2 Column Standards

```sql
CREATE TABLE shuttle_batches (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    session_id      BIGINT          NOT NULL,
    purchased_by    BIGINT          NOT NULL,
    quantity        INT             NOT NULL,
    remaining       INT             NOT NULL,              -- tồn kho hiện tại
    unit_price      DECIMAL(12, 2)  NOT NULL,              -- Tiền: DECIMAL(12,2)
    purchase_date   DATE            NOT NULL,
    notes           VARCHAR(500)    NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,                  -- soft delete: NULL = chưa xóa

    PRIMARY KEY (id),
    INDEX idx_shuttle_batches_session_id (session_id),
    CONSTRAINT fk_shuttle_batches_session FOREIGN KEY (session_id) REFERENCES sessions(id),
    CONSTRAINT fk_shuttle_batches_member  FOREIGN KEY (purchased_by) REFERENCES members(id)
);
```

**Bắt buộc:**
- Tiền tệ: `DECIMAL(12, 2)` — không dùng `FLOAT`/`DOUBLE`
- Thời gian: `DATETIME` (store UTC) — không dùng `TIMESTAMP` (giới hạn năm 2038)
- Mọi bảng phải có `created_at` và `updated_at`
- Soft delete: cột `deleted_at DATETIME NULL` — không xóa vật lý record có liên quan tài chính

### 4.3 Migration (Flyway)

- Tên file: `V{version}__{description}.sql`
  ```
  V1__create_initial_schema.sql
  V2__add_shuttle_batch_table.sql
  V3__add_session_task_checklist.sql
  ```
- **Không sửa file migration đã commit** — tạo file mới để ALTER
- Mỗi file migration chỉ thay đổi **một nhóm entity liên quan**
- Luôn có comment mô tả mục đích ở đầu file migration

### 4.4 Core Schema Overview

```
members
  ├── sessions (người tạo)
  └── shuttle_batches (người mua)

sessions
  ├── session_attendees     ← ai có mặt buổi đó
  ├── session_tasks         ← checklist: đặt sân, mua nước...
  ├── shuttle_batches       ← lô cầu nhập trong/cho buổi này
  ├── session_shuttle_usages← FIFO: buổi này dùng bao nhiêu từ lô nào
  └── expense_items         ← các khoản chi (sân, cầu, ăn sáng...)
        └── expense_participants ← ai chia khoản chi này

payments                    ← ghi nhận thanh toán của từng thành viên
```

---

## 5. Checklist Trước Khi Tạo PR

- [ ] Code đúng naming convention theo tài liệu này
- [ ] Không có `System.out.println` / `console.log` còn sót
- [ ] API response đúng envelope format `{ success, data, message, timestamp }`
- [ ] Tiền tệ: `BigDecimal` (backend) / `DECIMAL(12,2)` (DB) / không tính ở frontend
- [ ] Migration file đặt tên đúng, không sửa file cũ đã chạy
- [ ] Mọi entity/bảng có `created_at`, `updated_at`
- [ ] TypeScript không có `any`
- [ ] Dùng custom exception, không throw `RuntimeException` trực tiếp
- [ ] Không có logic tính toán tài chính trong frontend
