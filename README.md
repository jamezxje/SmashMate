# 🏸 SmashMate — Badminton Club Management System

**SmashMate** là ứng dụng web quản lý câu lạc bộ cầu lông **single-tenant**, giúp tự động hóa toàn bộ quy trình quản lý thành viên, xếp lịch sinh hoạt, theo dõi kho cầu theo FIFO, chia tiền buổi tập và quản lý công nợ.

---

## 🚀 Tính Năng Nổi Bật

### 1. 👥 Quản Lý Thành Viên
- **Phân quyền 3 vai trò:** `ADMIN` (Quản lý/Thủ quỹ), `MEMBER` (Thành viên chính thức), `GUEST` (Khách vãng lai).
- **Hỗ trợ thành viên Offline:** Admin có thể thêm thành viên chỉ với Tên/SĐT mà không bắt buộc tạo tài khoản đăng nhập. Sau này có thể gán email/mật khẩu để nâng cấp lên Account Member.
- **Khách vãng lai (GUEST):** Thêm nhanh trong từng buổi tập, hỗ trợ điểm danh và chia tiền độc lập.

### 2. 📅 Lịch Sinh Hoạt & Buổi Tập
- **Lịch định kỳ (Recurring Schedule):** Cấu hình lịch cố định hàng tuần (VD: Thứ 3, Thứ 5 từ 18:00–20:00).
- **Quản lý buổi tập (Session):** Sinh từ lịch định kỳ hoặc tạo đột xuất.
- **RSVP & Điểm danh:** Thành viên tự xác nhận tham gia hoặc Admin điểm danh thực tế tại sân.
- **Checklist công việc:** Phân công đặt sân, mua nước, mua cầu cho từng thành viên.

### 3. 🏸 Quản Lý Cầu & Tồn Kho (FIFO Inventory)
- **Nhập lô cầu (Shuttle Batches):** Quản lý người mua ứng trước, số lượng, đơn giá và tồn kho.
- **Tính chi phí cầu tự động theo FIFO:** Buổi tập dùng bao nhiêu quả sẽ tự động trừ lùi theo lô cũ nhất trước.
- **Hỗ trợ Override thủ công:** Cho phép Admin tự chỉ định lấy cầu từ lô cụ thể nếu cần.

### 4. 💰 Chi Phí & Chia Tiền Thô
- **Danh mục khoản chi (Expense Categories):** Cố định (Tiền sân, Tiền cầu) và mở rộng (Ăn uống, Khác).
- **Chia tiền theo nhóm tham gia:** Mỗi khoản chi có danh sách người tham gia riêng (VD: Tiền sân chia 9 người đánh, Tiền ăn sáng chia 8 người đi ăn).
- **Tự động làm tròn:** Làm tròn xuống đơn vị đồng và dồn phần lẻ cho người ứng tiền.

### 5. 💳 Công Nợ & Số Dư Thành Viên (Running Balance)
- **Hệ thống Ví/Số dư (Balance):** Tự động cấn trừ số tiền thừa của thành viên sang các buổi tập sau.
- **Xác nhận thanh toán:** Quản lý tiền nợ từng buổi, ghi nhận số tiền chuyển khoản thực tế. Nếu chuyển dư sẽ tự động tích lũy vào số dư thành viên.
- **Tích hợp QR Ngân hàng:** Hiển thị mã QR ngân hàng của CLB trực tiếp trên màn hình theo dõi công nợ.

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

| Layer | Công nghệ |
|-------|-----------|
| **Backend** | Java 21 (LTS), Spring Boot 3.x, Spring Security, JWT, Spring Data JPA |
| **Frontend** | ReactJS 18, Vite 5, Tailwind CSS 3, Zustand 4, Axios |
| **Database** | MySQL 8.x |
| **Migration** | Flyway |

---

## 📂 Cấu Trúc Tài Liệu

- 📄 [Design Specification](docs/superpowers/specs/2026-08-18-smashmate-design.md) — Tài liệu thiết kế chi tiết 6 module hệ thống.
- 📏 [Code Convention](docs/CODE_CONVENTION.md) — Quy chuẩn viết code cho Backend, Frontend và Database.
- 📋 [Implementation Plan (Phase 1)](docs/superpowers/plans/2026-08-18-smashmate-phase1-foundation.md) — Kế hoạch triển khai chi tiết cho Phase 1 (Foundation & Member Management).

---

## 📌 Quy Chuẩn Commit

Dự án tuân thủ theo [Conventional Commits](https://www.conventionalcommits.org/):

```text
<type>(<scope>): <short description>

Ví dụ:
  feat(member): add guest quick-create endpoint
  fix(shuttle): fix fifo calculation algorithm
  docs(readme): add project description and tech stack
```

---

## 📄 License

Dự án phát triển riêng cho Câu lạc bộ Cầu lông SmashMate.
