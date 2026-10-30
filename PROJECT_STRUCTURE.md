# HoLa Backend — Cấu Trúc Dự Án Hiện Tại

> **Stack**: Spring Boot 4.1.2 · Java 25 · MySQL 8.4 · Flyway · Spring Security (OAuth2 Resource Server / JWT) · Spring Mail (SMTP) · MapStruct 1.6.3 · Lombok · SpringDoc OpenAPI 3.0.0

---

## 1. Cây thư mục tổng thể

```text
hola-backend/
├── .env.example                                   # Bản mẫu biến môi trường (commit lên Git)
├── .env                                           # Biến môi trường chạy local (đã chặn trong .gitignore)
├── .gitignore
├── docker-compose.yml                             # Container MySQL 8.4 cho môi trường local
├── mvnw / mvnw.cmd                                # Maven Wrapper
├── pom.xml                                        # Cấu hình dependencies & annotation processors
├── PROJECT_STRUCTURE.md                           # Tài liệu cấu trúc dự án
└── src/
    ├── main/
    │   ├── java/org/anest/hola/
    │   │   ├── HolaBackendApplication.java        # Main class khởi chạy ứng dụng
    │   │   ├── common/                            # Thành phần dùng chung toàn hệ thống
    │   │   │   ├── dto/
    │   │   │   │   ├── ApiResponse.java           # Khung chuẩn JSON trả về {code, message, data, timestamp}
    │   │   │   │   └── PageResponse.java          # Khung chuẩn phân trang {items, page, size, totalElements...}
    │   │   │   ├── entity/
    │   │   │   │   └── BaseEntity.java            # Các cột audit dùng chung: createdAt, updatedAt, createdBy, updatedBy
    │   │   │   ├── exception/
    │   │   │   │   ├── AppException.java          # Custom RuntimeException mang theo ErrorCode
    │   │   │   │   ├── ErrorCode.java             # Danh mục mã lỗi (9xxx: Hệ thống, 1xxx: Auth, 2xxx: User)
    │   │   │   │   └── GlobalExceptionHandler.java# @RestControllerAdvice bắt và chuẩn hóa toàn bộ lỗi
    │   │   │   ├── i18n/
    │   │   │   │   ├── MessageKeys.java           # Hằng số key đa ngôn ngữ
    │   │   │   │   └── MessageUtils.java          # Tiện ích đọc chuỗi thông báo từ messages.properties
    │   │   │   └── security/
    │   │   │       └── SecurityUtils.java         # Tiện ích lấy userId hiện tại từ JWT Authentication
    │   │   │
    │   │   ├── config/                            # Cấu hình hạ tầng (Framework Config)
    │   │   │   ├── JpaAuditingConfig.java         # Bật JPA Auditing, tự động điền createdBy/updatedBy
    │   │   │   ├── OpenApiConfig.java             # Cấu hình Swagger UI & Bearer JWT Authentication
    │   │   │   ├── SchedulingConfig.java          # Bật @EnableScheduling cho các Cronjob
    │   │   │   └── SecurityConfig.java            # Cấu hình Spring Security, JWT Decoder/Encoder, CORS, Public URLs
    │   │   │
    │   │   └── modules/                           # Các phân hệ nghiệp vụ (Feature Modules)
    │   │       ├── auth/                          # Phân hệ Xác thực & Bảo mật tài khoản
    │   │       │   ├── controller/
    │   │       │   │   └── AuthController.java    # REST APIs: /api/v1/auth/*
    │   │       │   ├── dto/
    │   │       │   │   ├── RegisterRequest.java   # Payload đăng ký tài khoản
    │   │       │   │   ├── VerifyEmailRequest.java# Payload xác thực OTP email (6 chữ số)
    │   │       │   │   ├── ResendOtpRequest.java  # Payload yêu cầu gửi lại OTP xác thực email
    │   │       │   │   ├── ForgotPasswordRequest.java # Payload yêu cầu gửi OTP quên mật khẩu
    │   │       │   │   ├── ResetPasswordRequest.java  # Payload đặt lại mật khẩu bằng OTP
    │   │       │   │   ├── LoginRequest.java      # Payload đăng nhập
    │   │       │   │   ├── RefreshTokenRequest.java   # Payload làm mới Access Token
    │   │       │   │   ├── LogoutRequest.java     # Payload đăng xuất
    │   │       │   │   └── AuthResponse.java      # Response chứa accessToken, refreshToken, expiresIn, user
    │   │       │   ├── entity/
    │   │       │   │   ├── RefreshToken.java          # Bảng auth_refresh_tokens
    │   │       │   │   ├── EmailVerificationToken.java# Bảng email_verification_tokens (OTP xác thực email)
    │   │       │   │   └── PasswordResetToken.java    # Bảng password_reset_tokens (OTP đặt lại mật khẩu)
    │   │       │   ├── repository/
    │   │       │   │   ├── RefreshTokenRepository.java
    │   │       │   │   ├── EmailVerificationTokenRepository.java
    │   │       │   │   └── PasswordResetTokenRepository.java
    │   │       │   ├── scheduler/
    │   │       │   │   └── TokenCleanupScheduler.java # Cronjob dọn dẹp Refresh Token & OTP hết hạn/đã dùng
    │   │       │   └── service/
    │   │       │       ├── AuthService.java       # Nghiệp vụ đăng ký, OTP, khóa đăng nhập sai, quên mật khẩu
    │   │       │       └── JwtTokenService.java   # Sinh JWT, sinh Refresh Token, sinh OTP 6 số, băm SHA-256
    │   │       │
    │   │       ├── notification/                  # Phân hệ Gửi thông báo dùng chung
    │   │       │   └── service/
    │   │       │       └── EmailService.java      # Gửi email bất đồng bộ (@Async) qua SMTP cho mọi module
    │   │       │
    │   │       └── identity/                      # Phân hệ Quản lý Danh tính & Hồ sơ Người dùng
    │   │           ├── controller/
    │   │           │   └── UserController.java    # REST APIs: /api/v1/users/me, /api/v1/users/{id}
    │   │           ├── dto/
    │   │           │   ├── UserAuthDto.java       # DTO nội bộ phục vụ kiểm tra đăng nhập & khóa tạm thời
    │   │           │   └── UserResponse.java      # Thông tin người dùng trả về qua API
    │   │           ├── entity/
    │   │           │   ├── User.java              # Bảng users
    │   │           │   ├── UserDetail.java        # Bảng user_details (thông tin cá nhân mở rộng)
    │   │           │   ├── Role.java              # Bảng roles
    │   │           │   ├── RoleName.java          # Enum: USER, ADMIN, LANDLORD, EMPLOYER, MERCHANT
    │   │           │   └── UserStatus.java        # Enum: UNVERIFIED, ACTIVE, INACTIVE, LOCKED, DELETED
    │   │           ├── mapper/
    │   │           │   └── UserMapper.java        # MapStruct chuyển đổi User -> UserResponse
    │   │           ├── repository/
    │   │           │   ├── UserRepository.java
    │   │           │   ├── UserDetailRepository.java
    │   │           │   └── RoleRepository.java
    │   │           └── service/
    │   │               └── UserService.java       # Public contract tạo user, kích hoạt, đổi MK, đếm/khóa đăng nhập sai
    │   │
    │   └── resources/
    │       ├── application.properties             # Cấu hình chung & nạp file .env
    │       ├── application-dev.properties         # Cấu hình môi trường Dev (SQL log, Swagger UI)
    │       ├── application-prod.properties        # Cấu hình môi trường Production (tắt Swagger, bắt buộc biến MT)
    │       ├── messages.properties                # Chuỗi thông báo & thông báo lỗi tiếng Việt (i18n)
    │       └── db/migration/
    │           └── V1__init_schema.sql            # Khởi tạo 8 bảng Database qua Flyway
    │
    └── test/
        ├── java/org/anest/hola/
        │   ├── HolaBackendApplicationTests.java   # Kiểm tra nạp Spring Context & JPA Schema
        │   └── modules/
        │       ├── auth/
        │       │   ├── controller/AuthAndSecurityIntegrationTest.java # 24 Integration tests cho Auth & Security
        │       │   ├── scheduler/TokenCleanupSchedulerTest.java       # Unit test cho TokenCleanupScheduler
        │       │   └── service/AuthServiceTest.java                   # 17 Unit tests cho AuthService
        │       └── identity/
        │           └── service/UserServiceTest.java                   # 9 Unit tests cho UserService
        └── resources/
            └── application-test.properties        # Cấu hình H2 In-Memory DB chạy kiểm thử độc lập
```

---

## 2. Cấu trúc Database hiện tại (`V1__init_schema.sql`)

| STT | Tên bảng | Mục đích |
|---|---|---|
| 1 | `roles` | Lưu danh mục vai trò (`USER`, `ADMIN`, `LANDLORD`, `EMPLOYER`, `MERCHANT`). |
| 2 | `users` | Thông tin tài khoản chính, trạng thái (`status`), bộ đếm đăng nhập sai (`failed_login_attempts`) và thời gian khóa tạm (`locked_until`). |
| 3 | `user_details` | Thông tin hồ sơ mở rộng 1-1 với `users` (`mobile`, `gender`, `bod`, `address`). |
| 4 | `user_roles` | Bảng trung gian N-N gán vai trò cho người dùng (`user_id`, `role_id`). |
| 5 | `user_auth_providers` | Liên kết tài khoản mạng xã hội (`GOOGLE`, ...) phục vụ OAuth2. |
| 6 | `auth_refresh_tokens` | Lưu bản băm SHA-256 của Refresh Token (`token_hash`), thông tin thiết bị, thời hạn và trạng thái thu hồi. |
| 7 | `password_reset_tokens` | Lưu bản băm SHA-256 của mã OTP đặt lại mật khẩu (`otp_hash`), số lần nhập sai (`attempts`), thời gian hết hạn và thời điểm sử dụng (`used_at`). |
| 8 | `email_verification_tokens` | Lưu bản băm SHA-256 của mã OTP xác thực email (`otp_hash`), số lần nhập sai (`attempts`), thời gian hết hạn và thời điểm sử dụng (`used_at`). |

---

## 3. Danh sách REST API hiện có

| Phân hệ | Method | Đường dẫn | Quyền truy cập | Chức năng |
|---|---|---|---|---|
| **Auth** | `POST` | `/api/v1/auth/register` | Public | Đăng ký tài khoản (`UNVERIFIED`) và gửi mã OTP 6 số qua email |
| **Auth** | `POST` | `/api/v1/auth/verify-email` | Public | Nhập mã OTP 6 số để kích hoạt tài khoản (`ACTIVE`) và nhận JWT |
| **Auth** | `POST` | `/api/v1/auth/resend-verification-otp` | Public | Yêu cầu gửi lại mã OTP xác thực email mới |
| **Auth** | `POST` | `/api/v1/auth/forgot-password` | Public | Yêu cầu gửi mã OTP 6 số để đặt lại mật khẩu |
| **Auth** | `POST` | `/api/v1/auth/reset-password` | Public | Nhập mã OTP 6 số + mật khẩu mới để đặt lại mật khẩu |
| **Auth** | `POST` | `/api/v1/auth/login` | Public | Đăng nhập bằng email & mật khẩu (khóa 15 phút nếu sai 5 lần) |
| **Auth** | `POST` | `/api/v1/auth/refresh` | Public | Đổi Refresh Token cũ lấy cặp Access/Refresh Token mới (Token Rotation) |
| **Auth** | `POST` | `/api/v1/auth/logout` | Public | Thu hồi Refresh Token hiện tại |
| **User** | `GET` | `/api/v1/users/me` | Authenticated | Lấy thông tin hồ sơ của người dùng đang đăng nhập |
| **User** | `GET` | `/api/v1/users/{id}` | `ROLE_ADMIN` | Xem thông tin người dùng bất kỳ theo ID |

