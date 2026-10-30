# HoLa Backend Architecture

> **Tài liệu kiến trúc chính thức của `hola-backend`**\
> Phiên bản: 1.0\
> Trạng thái: Architecture baseline\
> Phạm vi: Backend Spring Boot của nền tảng HoLa

---

## 1. Mục tiêu và phạm vi

Tài liệu này xác định các nguyên tắc kiến trúc, ranh giới module, quy
tắc phụ thuộc, quy ước tổ chức mã nguồn và tiêu chuẩn kỹ thuật cho
`hola-backend`.

HoLa được định hướng là một nền tảng tích hợp nhiều dịch vụ phục vụ
người sống, học tập và làm việc tại Hòa Lạc. Các phân hệ dự kiến gồm:

- **HoLaFood**: tìm kiếm và khám phá quán ăn.
- **HoLaHome**: tìm kiếm phòng trọ, nhà cho thuê và người ở ghép.
- **HoLaWork**: tìm kiếm việc làm và tuyển dụng.

Backend cần hỗ trợ mở rộng các phân hệ này mà không tạo ra sự phụ thuộc
chéo khó kiểm soát.

Tài liệu phân biệt rõ:

- **Hiện trạng**: những thành phần đã được mô tả trong
  `PROJECT_STRUCTURE.md`.
- **Định hướng kiến trúc**: các quy tắc và module mục tiêu. Việc xuất
  hiện trong tài liệu không có nghĩa là đã được triển khai.

## 2. Nguyên tắc kiến trúc

### 2.1. Modular Monolith

HoLa sử dụng kiến trúc **Modular Monolith**:

- Một Spring Boot application và một quy trình triển khai backend.
- Các module được phân chia theo năng lực nghiệp vụ (business
  capability).
- Mỗi module sở hữu logic nghiệp vụ và các thành phần persistence
  thuộc phạm vi của mình.
- Giao tiếp giữa các module thông qua contract/service công khai,
  không truy cập trực tiếp implementation nội bộ.
- Chỉ cân nhắc tách microservice khi có nhu cầu thực tế về triển khai
  độc lập, mở rộng tải, cô lập lỗi hoặc quyền sở hữu dữ liệu.

Không tạo microservice chỉ vì một module có tên hoặc có database table
riêng.

### 2.2. Feature-oriented organization

Tổ chức mã nguồn theo module nghiệp vụ, không chia toàn bộ ứng dụng
thành các thư mục technical layer dùng chung như `controllers/`,
`services/`, `repositories/`.

Mỗi module có thể chứa các package phù hợp với nhu cầu:

- `controller`
- `dto`
- `entity`
- `mapper`
- `repository`
- `service`
- `scheduler` hoặc `event` khi thực sự cần

Không bắt buộc module nào cũng phải có đủ tất cả package trên.

### 2.3. Separation of concerns

- Controller xử lý giao tiếp HTTP, không chứa nghiệp vụ phức tạp.
- Service điều phối và thực thi nghiệp vụ.
- Repository chịu trách nhiệm truy cập dữ liệu.
- Entity biểu diễn mô hình persistence, không dùng trực tiếp làm API
  response.
- DTO định nghĩa contract đầu vào/đầu ra.
- Mapper chuyển đổi giữa các mô hình khi cần.
- Configuration chỉ chứa cấu hình framework/hạ tầng, không chứa nghiệp
  vụ.

### 2.4. Explicit boundaries

Mỗi module phải có trách nhiệm rõ ràng, dữ liệu sở hữu rõ ràng và API
nội bộ rõ ràng. Không để `common` trở thành nơi chứa các class không
biết đặt ở đâu.

## 3. Cấu trúc package mục tiêu

Cấu trúc dưới đây là định hướng mở rộng. Chỉ tạo module/package khi có
nghiệp vụ thực tế.

```text
org.anest.hola/
├── HolaBackendApplication.java
│
├── common/
│   ├── dto/
│   ├── entity/
│   ├── exception/
│   ├── i18n/
│   └── security/
│
├── config/
│   ├── persistence/
│   ├── security/
│   ├── openapi/
│   └── scheduling/
│
└── modules/
    ├── identity/
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── mapper/
    │   ├── repository/
    │   └── service/
    │
    ├── auth/
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── repository/
    │   ├── scheduler/
    │   └── service/
    │
    ├── notification/
    │   └── service/
    │
    ├── location/
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── mapper/
    │   ├── repository/
    │   └── service/
    │
    ├── food/
    ├── home/
    └── work/
```

### 3.1. `common`

Chứa các thành phần kỹ thuật thực sự dùng chung và không thuộc riêng
module nghiệp vụ.

Hiện trạng được mô tả:

- `dto/ApiResponse.java`
- `dto/PageResponse.java`
- `entity/BaseEntity.java`
- `exception/AppException.java`
- `exception/ErrorCode.java`
- `exception/GlobalExceptionHandler.java`
- `i18n/MessageKeys.java`
- `i18n/MessageUtils.java`
- `security/SecurityUtils.java`

Quy tắc:

- Chỉ đưa class vào `common` khi có ít nhất hai khu vực sử dụng hợp lý
  hoặc nó là cross-cutting concern.
- Không đặt DTO nghiệp vụ, Entity nghiệp vụ, Repository nghiệp vụ hoặc
  Service nghiệp vụ tại đây.
- Tránh để `common` phụ thuộc vào `modules/*`.
- Giữ các tiện ích nhỏ, có mục đích cụ thể; không tạo một
  `CommonService` tổng hợp nhiều trách nhiệm.

### 3.2. `config`

Chứa cấu hình framework và hạ tầng ở cấp ứng dụng, ví dụ:

- JPA auditing.
- Spring Security.
- OpenAPI.
- Scheduling.

Có thể nhóm theo chủ đề khi số lượng cấu hình tăng. Không cần tạo
package con rỗng từ sớm.

### 3.3. `identity`

Quản lý danh tính người dùng, thông tin cá nhân, trạng thái tài khoản, vai trò và hồ sơ người dùng. Không chịu trách nhiệm thực hiện quy trình đăng nhập.

Phạm vi sở hữu của `identity`:

- `User`, `UserDetail` và thông tin hồ sơ người dùng.
- `Role`, `RoleName` và các thông tin phân quyền nền tảng.
- `UserStatus` (trạng thái tài khoản).
- `UserRepository`, `UserDetailRepository`, `RoleRepository`.
- Tra cứu và cập nhật hồ sơ.
- Public service / contract (`UserService`) để `auth` và các module khác (`food`, `home`, `work`) tra cứu hoặc thao tác thông tin người dùng cần thiết.

Nguyên tắc bắt buộc:

- `identity` sở hữu `User` và toàn bộ các bảng/thông tin liên quan.
- `auth` và các module khác không được truy cập trực tiếp `UserRepository`, `RoleRepository`, `UserDetailRepository` hoặc sửa trực tiếp Entity của `identity`.
- Mọi giao tiếp xuyên module phải đi qua public service hoặc contract được cung cấp bởi `identity`.

Phân biệt `auth` và `identity`:

| Module     | Trách nhiệm                                                                                         | Hiểu đơn giản                    |
| ---------- | --------------------------------------------------------------------------------------------------- | -------------------------------- |
| `auth`     | Đăng nhập, đăng ký, xác thực JWT, refresh token, logout, xác minh email, quên và đặt lại mật khẩu.  | Người dùng chứng minh họ là ai.  |
| `identity` | Quản lý danh tính người dùng, thông tin cá nhân, trạng thái tài khoản, vai trò và hồ sơ người dùng. | Hệ thống quản lý người đó là ai. |

Hiện trạng: Đã được triển khai tại `org.anest.hola.modules.identity`.

### 3.4. `auth`

Quản lý xác thực và vòng đời thông tin xác thực:

- Đăng ký và đăng nhập.
- Email verification OTP.
- Forgot/reset password.
- Access Token và Refresh Token.
- Refresh Token rotation và revoke.
- Chính sách khóa đăng nhập.
- Các scheduler liên quan đến dữ liệu xác thực.

Hiện trạng: module `auth` đã có controller, DTO, entity, repository và
service cho các chức năng được mô tả trong `PROJECT_STRUCTURE.md`.

Không đặt hồ sơ người dùng hoặc nghiệp vụ của HoLaFood/HoLaHome trong
module này.

### 3.5. `notification`

Quản lý khả năng gửi thông báo dùng chung cho toàn bộ các module (`auth`, `food`, ...), chẳng hạn gửi email bất đồng bộ (`@Async`) qua SMTP hoặc mở rộng thêm các kênh thông báo khác trong tương lai.

Hiện trạng: Đã được triển khai tại `org.anest.hola.modules.notification.service.EmailService`.

Nguyên tắc:

-   Các module nghiệp vụ (`auth`, `food`, ...) chỉ gọi contract của `notification` (`EmailService`), không phụ thuộc trực tiếp vào chi tiết SMTP hoặc `JavaMailSender`.
-   Các module khác không được phụ thuộc vào `auth` để gửi email.
-   Không cần xây dựng hệ thống hàng đợi (queue) phức tạp khi nhu cầu hiện tại chỉ cần gửi email bất đồng bộ (`@Async`).

### 3.6. `location`

Quản lý dữ liệu địa lý và địa chỉ dùng chung cho các module:

- Dữ liệu đơn vị hành chính.
- Địa chỉ và thành phần địa chỉ.
- Tra cứu địa điểm theo nhu cầu nghiệp vụ.
- Các contract tra cứu địa chỉ cho `identity`, `food`, `home` và
  `work`.

Không để mỗi module tự xây dựng một mô hình tỉnh/thành, phường/xã không
tương thích với nhau.

Không mặc định mọi địa chỉ đều phải là một entity dùng chung. Cần phân
biệt dữ liệu hành chính chuẩn hóa với địa chỉ hiển thị hoặc mô tả vị trí
của một đối tượng nghiệp vụ.

### 3.7. `food`, `home`, `work`

Mỗi module sở hữu nghiệp vụ riêng:

- `food`: quán ăn, thông tin kinh doanh ẩm thực, tìm kiếm và đánh giá.
- `home`: phòng trọ, nhà cho thuê, thông tin cho thuê và người ở ghép.
- `work`: tin tuyển dụng, công việc và thông tin tuyển dụng.

Các module này không được truy cập trực tiếp repository hoặc sửa entity
của nhau. Nếu cần dữ liệu từ module khác, sử dụng contract công khai.

## 4. Quy tắc phụ thuộc giữa các module

### 4.1. Quy tắc bắt buộc

1.  Module A không được gọi trực tiếp Repository của module B.
2.  Module A không được sửa hoặc quản lý vòng đời Entity do module B sở
    hữu.
3.  Không trả Entity của module này trực tiếp qua API của module khác.
4.  Không tạo dependency vòng giữa các module.
5.  Không dùng `common` để né tránh việc xác định đúng module sở hữu
    nghiệp vụ.
6.  Contract công khai phải nhỏ, ổn định và chỉ cung cấp dữ liệu/chức
    năng mà module gọi thực sự cần.
7.  Không chia sẻ persistence model chỉ để giảm số lượng class.

### 4.2. Cách giao tiếp

Ưu tiên lời gọi service/facade công khai trong cùng ứng dụng đối với
nghiệp vụ đồng bộ.

Ví dụ về ý tưởng:

```text
food service
    |
    +----> location public service / contract
    |
    +----> identity public service / contract
```

Tên và API cụ thể phải được thiết kế theo use case thực tế. Không tạo
một lớp facade tổng quát cho mọi module nếu không có nhu cầu.

Có thể dùng application event cho các tác vụ cần tách rời hoặc xử lý
sau, nhưng không dùng event để làm phức tạp các lời gọi đồng bộ đơn
giản.

### 4.3. Kiểm soát dependency

Khi dự án lớn hơn, cân nhắc dùng ArchUnit hoặc cơ chế kiểm tra kiến trúc
tương đương để tự động phát hiện:

- Dependency vòng.
- Module truy cập package nội bộ của module khác.
- Controller phụ thuộc trực tiếp vào Repository.
- Entity hoặc DTO nội bộ bị sử dụng xuyên module trái quy tắc.

## 5. Quy tắc thiết kế API

### 5.1. REST API

- Sử dụng prefix `/api/v1`.
- Dùng danh từ số nhiều cho resource khi phù hợp, ví dụ
  `/api/v1/users`.
- Dùng HTTP method đúng ngữ nghĩa: `GET`, `POST`, `PUT`, `PATCH`,
  `DELETE`.
- Không đưa logic nghiệp vụ vào URL.
- Không để API của module này trở thành đường truy cập trực tiếp vào
  Repository module khác.

Các API hiện tại được mô tả trong `PROJECT_STRUCTURE.md` sử dụng
`/api/v1/auth/*` và `/api/v1/users/*`.

### 5.2. Request và Response

- Dùng Request DTO cho dữ liệu đầu vào.
- Dùng Response DTO cho dữ liệu trả về.
- Validate input tại ranh giới API bằng Jakarta Bean Validation.
- Không nhận trực tiếp JPA Entity từ request.
- Không trả trực tiếp JPA Entity trong response.
- Không trả thông tin nhạy cảm như password hash, OTP hash hoặc
  refresh token hash.

### 5.3. Response và lỗi

Mọi JSON REST API của ứng dụng (ngoại trừ các endpoint hạ tầng như Actuator/OpenAPI hoặc xuất file binary) **bắt buộc** bọc dữ liệu trong `ApiResponse<T>` với các trường `code`, `message`, `data`, `timestamp` (và `ApiResponse<PageResponse<T>>` cho dữ liệu phân trang).

Quy ước bắt buộc cho toàn bộ API:

- **Thành công (`code = 1000`)**:
  - `GET`, `PUT`, `PATCH`, `POST` (action): trả về `200 OK` với `ApiResponse<T>`.
  - `POST` (khởi tạo resource): trả về `201 Created` với `ApiResponse<T>`.
  - `DELETE`: trả về `200 OK` với `ApiResponse<Void>` (không dùng `204 No Content`).
- **Lỗi nghiệp vụ**: ném `AppException(ErrorCode.X)` tại tầng Service; `GlobalExceptionHandler` tự động chuẩn hóa thành `ApiResponse` với HTTP status và mã lỗi tương ứng.
- **Validation error (`code = 9002`, HTTP `400`)**: `data` trả về map `{field: message}` chỉ rõ trường không hợp lệ mà không làm lộ dữ liệu nhạy cảm.
- **Bảo mật thông tin lỗi**: không trả stack trace hoặc chi tiết nội bộ cho client production.

#### Quy hoạch dải mã lỗi (`ErrorCode`)

Để tránh xung đột mã lỗi khi phát triển song song nhiều module, `ErrorCode` được phân bổ cố định theo từng phân hệ:

| Dải mã (`code`) | Module sở hữu        | Mô tả                                                                    |
| --------------- | -------------------- | ------------------------------------------------------------------------ |
| `1000`          | Toàn hệ thống        | Thành công (`ApiResponse.DEFAULT_SUCCESS_CODE`)                          |
| `1xxx`          | `auth`               | Xác thực, JWT, Refresh Token, OTP, Khóa đăng nhập                        |
| `2xxx`          | `identity`           | Người dùng, Hồ sơ cá nhân, Vai trò, Trạng thái tài khoản                 |
| `3xxx`          | `location` & `media` | Địa giới hành chính, Địa điểm, Upload/Lưu trữ file                       |
| `4xxx`          | `food` (HoLaFood)    | Quán ăn, Thực đơn, Đánh giá ẩm thực                                      |
| `9xxx`          | `common` / System    | Lỗi hệ thống, Validation (`9002`), Not Found (`9003`), Conflict (`9005`) |

### 5.4. Pagination, sorting và filtering

- Thống nhất quy ước page index (khuyến nghị bắt đầu từ `0` nếu dùng
  trực tiếp Spring Data Pageable).
- Giới hạn page size tối đa.
- Chỉ cho phép sort theo các field được hỗ trợ, không truyền tùy ý tên
  thuộc tính vào truy vấn.
- Thiết kế filter theo nhu cầu use case, tránh tạo API tìm kiếm quá
  tổng quát ngay từ đầu.

## 6. Quy tắc persistence và database

### 6.1. Database migration

Flyway là nguồn quản lý thay đổi schema duy nhất.

- Mọi thay đổi schema phải có migration mới.
- Không sửa migration đã được áp dụng ở môi trường dùng
  chung/production.
- Dùng tên migration theo quy ước Flyway, ví dụ
  `V2__add_food_tables.sql`.
- Migration phải có thứ tự, có thể review và có thể chạy lại theo cơ
  chế của Flyway.
- Dữ liệu seed cần thiết cho hệ thống phải được quản lý có chủ đích.

### 6.2. Hibernate

Đặt:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Không sử dụng `create`, `create-drop` hoặc `update` trong môi trường
production.

Hibernate chỉ xác thực mapping với schema; Flyway chịu trách nhiệm thay
đổi schema.

### 6.3. Entity và mapping

- Entity thuộc module sở hữu dữ liệu.
- Dùng quan hệ JPA khi thực sự cần thiết; tránh tạo object graph quá
  lớn.
- Cân nhắc lazy loading cho quan hệ, và chủ động fetch dữ liệu theo
  use case.
- Không serialize Entity trực tiếp.
- Không dùng cascade rộng nếu chưa xác định rõ vòng đời dữ liệu.
- Có index, unique constraint, foreign key và check constraint phù hợp
  với quy tắc dữ liệu.
- Đặt tên bảng/cột nhất quán và tránh phụ thuộc vào tên mặc định khó
  đoán của ORM.

### 6.4. Audit

`BaseEntity` hiện được mô tả là chứa `createdAt`, `updatedAt`,
`createdBy`, `updatedBy`.

Các module có thể kế thừa nếu phù hợp. Không bắt buộc mọi bảng đều có
đầy đủ audit columns; quyết định dựa trên nhu cầu truy vết và vòng đời
dữ liệu.

Cần thống nhất timezone và cách lưu/serialize thời gian giữa backend và
frontend.

### 6.5. Database integration tests

H2 có thể dùng cho test nhanh, nhưng không thay thế hoàn toàn việc kiểm
thử trên database production tương ứng.

Với các luồng phụ thuộc MySQL, Flyway, constraint, collation hoặc SQL
đặc thù, ưu tiên Testcontainers với phiên bản MySQL tương thích môi
trường triển khai.

## 7. Quy tắc bảo mật

- Bảo vệ endpoint theo nguyên tắc deny-by-default.
- Chỉ khai báo public endpoint khi có lý do nghiệp vụ.
- Phân biệt authentication và authorization.
- Kiểm tra quyền ở backend; không tin tưởng việc ẩn/hiện nút ở
  frontend.
- Không commit `.env`, secret, private key hoặc credential.
- `.env.example` chỉ chứa tên biến và giá trị mẫu không nhạy cảm.
- Không log password, OTP, token thô hoặc dữ liệu xác thực nhạy cảm.
- Refresh token nên được lưu dưới dạng hash và có cơ chế
  revoke/rotation.
- Cấu hình CORS theo origin được phép; không dùng wildcard tùy tiện
  trong production.
- Có rate limiting hoặc cơ chế chống lạm dụng phù hợp cho login, OTP,
  resend OTP và reset password.
- Thông báo lỗi authentication không nên giúp dò tìm email/tài khoản
  hợp lệ.
- Quản lý secret bằng môi trường triển khai hoặc secret manager phù
  hợp.

Role/permission phải phản ánh quyền truy cập hệ thống. Không mặc định
mọi vai trò nghiệp vụ như landlord, employer hay merchant đều tương
đương với một tập quyền cố định mà không có mô hình authorization rõ
ràng.

## 8. Quy tắc service và transaction

- Đặt transaction boundary tại service/application use case.
- Dùng `@Transactional` rõ ràng cho thao tác ghi dữ liệu cần tính
  nguyên tử.
- Đặt `readOnly = true` cho use case chỉ đọc khi phù hợp.
- Không giữ transaction mở trong lúc gọi dịch vụ bên ngoài lâu như
  SMTP hoặc HTTP.
- Xác định cách xử lý lỗi khi kết hợp database transaction với side
  effect bên ngoài.
- Không bắt và bỏ qua exception một cách im lặng.
- Không tạo service chỉ để chuyển tiếp một lời gọi nếu không có giá
  trị về ranh giới hoặc điều phối.

## 9. Quy ước code và naming

- Tuân theo Java naming conventions.
- Class/interface: `PascalCase`.
- Method/field/package: `camelCase` (package viết thường).
- Constant: `UPPER_SNAKE_CASE`.
- Tên class thể hiện vai trò hoặc nghiệp vụ, ví dụ `UserService`,
  `AuthController`, `FoodResponse`.
- Tránh tên mơ hồ như `CommonManager`, `DataHelper`, `BaseService` nếu
  không có trách nhiệm cụ thể.
- Giữ method ngắn và tập trung vào một trách nhiệm.
- Không lạm dụng Lombok; ưu tiên constructor injection.
- Không dùng field injection.
- Không đưa business rule vào mapper hoặc controller.
- Không dùng `Optional` làm field của Entity hoặc DTO.
- Không để magic number/string cho các chính sách nghiệp vụ; đưa thành
  constant hoặc cấu hình phù hợp.

## 10. Testing strategy

Phân loại test theo mục tiêu:

---

Loại test Mục tiêu Công cụ định hướng

---

Unit test Kiểm tra logic JUnit 5, Mockito
class/service độc lập

MVC/API test Kiểm tra HTTP contract, Spring Boot Test,
validation, security MockMvc
filter

Integration test Kiểm tra nhiều thành Spring Boot Test
phần Spring phối hợp

Database integration Kiểm tra migration, Testcontainers
mapping, constraint  
 trên MySQL

Architecture test Kiểm tra dependency và ArchUnit
module boundary

---

Yêu cầu:

- Test các nhánh nghiệp vụ quan trọng, không chỉ test happy path.
- Test authorization: anonymous, user thường và admin.
- Test validation và error response.
- Test token expiration, reuse, revoke và rotation.
- Test các quy tắc unique/foreign key và transaction.
- Test Flyway migration trên database tương thích production.
- Không coi số lượng test là thước đo duy nhất; ưu tiên chất lượng
  assertion và mức độ rủi ro được bao phủ.

## 11. Configuration và môi trường

Hiện trạng được mô tả gồm:

- `application.properties`
- `application-dev.properties`
- `application-prod.properties`
- `application-test.properties`
- `.env.example`
- `.env` local (được `.gitignore` loại trừ)

Nguyên tắc:

- Giá trị mặc định không nhạy cảm có thể đặt trong cấu hình.
- Secret phải được inject từ environment hoặc secret manager.
- Không commit `.env` thật.
- Dev và production phải có cấu hình bảo mật phù hợp từng môi trường.
- Production không bật SQL parameter logging hoặc Swagger UI công khai
  nếu không có kiểm soát phù hợp.
- Cấu hình test phải độc lập và có thể tái lập.

## 12. API documentation và observability

- Duy trì OpenAPI cho các API public.
- DTO, validation và response code cần được mô tả rõ.
- Log có cấu trúc và có correlation/request ID khi phù hợp.
- Không log dữ liệu nhạy cảm.
- Theo dõi lỗi ứng dụng, thời gian phản hồi và tình trạng kết nối
  database.
- Health endpoint không được tiết lộ secret hoặc thông tin hạ tầng
  nhạy cảm.

## 13. Quy trình thêm module mới

Khi thêm một module, thực hiện theo thứ tự:

1.  Xác định business capability và phạm vi sở hữu của module.
2.  Xác định dữ liệu và bảng mà module sở hữu.
3.  Xác định contract cần cung cấp và dependency cần sử dụng.
4.  Thiết kế migration Flyway.
5.  Tạo Entity, Repository và Service cần thiết.
6.  Tạo Request/Response DTO, Mapper và Controller nếu có API.
7.  Thêm authorization và validation.
8.  Viết unit/integration/database tests phù hợp.
9.  Cập nhật OpenAPI và tài liệu liên quan.
10. Review dependency để bảo đảm không vi phạm module boundary.

Không tạo module chỉ để gom một vài class kỹ thuật không có ranh giới
nghiệp vụ.

## 14. Quy trình thay đổi schema

1.  Xác định module sở hữu bảng/dữ liệu.
2.  Tạo migration Flyway mới.
3.  Cập nhật Entity và mapping.
4.  Cập nhật Repository/Service/DTO liên quan.
5.  Kiểm tra tương thích với dữ liệu hiện có.
6.  Viết hoặc cập nhật database integration tests.
7.  Chạy test và xác minh Hibernate `ddl-auto=validate`.
8.  Review migration trước khi merge.

Với thay đổi có khả năng gây downtime hoặc không tương thích ngược, cần
lập kế hoạch migration theo nhiều bước (expand-and-contract) thay vì
thay đổi phá vỡ ngay lập tức.

## 15. Hiện trạng và các việc cần xác minh

Dựa trên tài liệu `PROJECT_STRUCTURE.md`, hiện trạng được mô tả:

- Spring Boot 4.1.2, Java 25, MySQL 8.4.
- Flyway quản lý schema.
- Spring Security OAuth2 Resource Server/JWT.
- Các module hiện có: `auth`, `identity`, `notification`.
- Common DTO, BaseEntity, exception, i18n và security utility.
- Các cấu hình JPA auditing, OpenAPI, scheduling và security.
- Có unit tests, integration tests và H2 test profile.

Các nội dung sau là đề xuất hoặc cần kiểm tra trực tiếp trong source
code, không được mặc định là đã hoàn thành:

- Xây dựng module `location`, `media`.
- Xây dựng module `food`.
- Áp dụng Testcontainers/MySQL cho database integration tests.
- Kiểm tra module boundaries bằng ArchUnit.
- Xác minh entity/repository cho `user_auth_providers`.
- Xác minh cấu hình `ddl-auto=validate`, CORS, rate limiting, secret
  handling và logging.
- Xác minh toàn bộ migration chạy đúng trên MySQL mục tiêu.

## 16. Checklist review kiến trúc

Trước khi merge một thay đổi đáng kể, kiểm tra:

- [ ] Code nằm trong đúng module sở hữu nghiệp vụ.
- [ ] Không truy cập trực tiếp Repository/Entity nội bộ của module
      khác.
- [ ] Không tạo dependency vòng.
- [ ] Controller không chứa business logic phức tạp.
- [ ] API dùng DTO, có validation và authorization phù hợp.
- [ ] Không trả dữ liệu nhạy cảm.
- [ ] Schema thay đổi thông qua migration Flyway mới.
- [ ] Entity mapping tương thích với schema và `ddl-auto=validate`.
- [ ] Transaction boundary hợp lý.
- [ ] Có test cho happy path, error path và security path quan trọng.
- [ ] Không commit secret hoặc log credential/token.
- [ ] Tài liệu API và tài liệu kiến trúc được cập nhật khi cần.

---

## 17. Quy tắc ưu tiên khi có xung đột

Khi đưa ra quyết định kỹ thuật, ưu tiên theo thứ tự:

1.  Tính đúng đắn nghiệp vụ và an toàn dữ liệu.
2.  Bảo mật và quyền riêng tư.
3.  Ranh giới module và khả năng bảo trì.
4.  Tính tương thích API/database.
5.  Hiệu năng có bằng chứng hoặc yêu cầu thực tế.
6.  Sự đơn giản và nhất quán với codebase hiện tại.

Không refactor diện rộng chỉ vì sở thích cá nhân hoặc xu hướng công
nghệ. Trước khi thay đổi convention hiện có, phải kiểm tra source code,
test và các tài liệu liên quan.

**Nguyên tắc cuối cùng:** HoLa phát triển theo hướng một nền tảng nhiều
nghiệp vụ trên cùng một backend có ranh giới module rõ ràng. Ưu tiên
thiết kế đơn giản, kiểm soát dependency và bảo đảm dữ liệu; chỉ tăng mức
độ phức tạp khi nhu cầu thực tế chứng minh điều đó.
