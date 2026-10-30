package org.anest.hola.modules.auth.controller;

import org.anest.hola.common.exception.AppException;
import org.anest.hola.common.exception.ErrorCode;
import org.anest.hola.common.exception.GlobalExceptionHandler;
import org.anest.hola.config.SecurityConfig;
import org.anest.hola.modules.auth.dto.AuthResponse;
import org.anest.hola.modules.auth.service.AuthService;
import org.anest.hola.modules.auth.service.JwtTokenService;
import org.anest.hola.modules.user.controller.UserController;
import org.anest.hola.modules.user.dto.UserResponse;
import org.anest.hola.modules.user.entity.RoleName;
import org.anest.hola.modules.user.entity.UserStatus;
import org.anest.hola.modules.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({SecurityConfig.class, JwtTokenService.class, GlobalExceptionHandler.class})
class AuthAndSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @Test
    void register_WithValidPayload_ShouldReturn201CreatedWithUnverifiedUser() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.UNVERIFIED)
                .roles(Set.of(RoleName.USER))
                .build();

        when(authService.register(any())).thenReturn(userResponse);

        String requestJson = """
                {
                  "email": "student@hola.edu.vn",
                  "password": "Password123!",
                  "fullName": "Nguyen Van A"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Đăng ký tài khoản thành công, vui lòng kiểm tra email để lấy mã xác thực OTP"))
                .andExpect(jsonPath("$.data.email").value("student@hola.edu.vn"))
                .andExpect(jsonPath("$.data.status").value("UNVERIFIED"));
    }

    @Test
    void verifyEmail_WithValidOtp_ShouldReturn200WithTokens() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(RoleName.USER))
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("jwt-access-token")
                .refreshToken("opaque-refresh-token")
                .expiresIn(3600)
                .user(userResponse)
                .build();

        when(authService.verifyEmail(any())).thenReturn(authResponse);

        String requestJson = """
                {
                  "email": "student@hola.edu.vn",
                  "otp": "123456"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Xác thực email thành công"))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-access-token"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"));
    }

    @Test
    void verifyEmail_WithInvalidOtpFormat_ShouldReturn400ValidationError() throws Exception {
        String invalidJson = """
                {
                  "email": "student@hola.edu.vn",
                  "otp": "12ab"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9002))
                .andExpect(jsonPath("$.data.otp").value("Mã OTP phải gồm đúng 6 chữ số"));
    }

    @Test
    void verifyEmail_WhenMaxAttemptsExceeded_ShouldReturn400WithCode1010() throws Exception {
        when(authService.verifyEmail(any())).thenThrow(new AppException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED));

        String requestJson = """
                {
                  "email": "student@hola.edu.vn",
                  "otp": "000000"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1010))
                .andExpect(jsonPath("$.message").value("Bạn đã nhập sai mã OTP quá 5 lần, vui lòng yêu cầu gửi lại mã mới"));
    }

    @Test
    void forgotAndResetPassword_WithValidPayloads_ShouldReturn200Ok() throws Exception {
        doNothing().when(authService).forgotPassword(any());
        doNothing().when(authService).resetPassword(any());

        String forgotJson = """
                {
                  "email": "student@hola.edu.vn"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forgotJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Nếu email tồn tại trong hệ thống, mã OTP đặt lại mật khẩu đã được gửi"));

        String resetJson = """
                {
                  "email": "student@hola.edu.vn",
                  "otp": "654321",
                  "newPassword": "NewPassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công"));
    }

    @Test
    void register_WithInvalidPayload_ShouldReturn400ValidationError() throws Exception {
        String invalidJson = """
                {
                  "email": "not-an-email",
                  "password": "short",
                  "fullName": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9002))
                .andExpect(jsonPath("$.message").value("Dữ liệu đầu vào không hợp lệ"))
                .andExpect(jsonPath("$.data.email").value("Định dạng email không hợp lệ"))
                .andExpect(jsonPath("$.data.password").value("Mật khẩu phải từ 8 đến 72 ký tự"))
                .andExpect(jsonPath("$.data.fullName").value("Họ và tên không được để trống"));
    }

    @Test
    void register_WithEmptyPassword_ShouldDeterministicallyPreferNotBlankOverSize() throws Exception {
        String emptyPasswordJson = """
                {
                  "email": "student@hola.edu.vn",
                  "password": "",
                  "fullName": "Nguyen Van A"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyPasswordJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9002))
                .andExpect(jsonPath("$.data.password").value("Mật khẩu không được để trống"));
    }

    @Test
    void getCurrentUser_WithoutToken_ShouldReturn401Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("Vui lòng đăng nhập để tiếp tục"));
    }

    @Test
    void getCurrentUser_WithValidUserToken_ShouldReturn200Profile() throws Exception {
        UserResponse user = UserResponse.builder()
                .id(42L)
                .email("user42@hola.edu.vn")
                .roles(Set.of(RoleName.USER, RoleName.LANDLORD))
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtTokenService.generateAccessToken(user);

        when(userService.getCurrentUserProfile(42L)).thenReturn(UserResponse.builder()
                .id(42L)
                .email("user42@hola.edu.vn")
                .fullName("Landlord User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(RoleName.USER, RoleName.LANDLORD))
                .build());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.email").value("user42@hola.edu.vn"));
    }

    @Test
    void nonWhitelistedAuthEndpoint_WithoutToken_ShouldReturn401Unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void getUserById_WithUserRole_ShouldReturn403Forbidden() throws Exception {
        UserResponse regularUser = UserResponse.builder()
                .id(42L)
                .email("user42@hola.edu.vn")
                .roles(Set.of(RoleName.USER))
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtTokenService.generateAccessToken(regularUser);

        mockMvc.perform(get("/api/v1/users/99")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void getUserById_WithAdminRole_ShouldReturn200Ok() throws Exception {
        UserResponse adminUser = UserResponse.builder()
                .id(1L)
                .email("admin@hola.edu.vn")
                .roles(Set.of(RoleName.USER, RoleName.ADMIN))
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtTokenService.generateAccessToken(adminUser);

        when(userService.getById(99L)).thenReturn(UserResponse.builder()
                .id(99L)
                .email("target@hola.edu.vn")
                .fullName("Target User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(RoleName.USER))
                .build());

        mockMvc.perform(get("/api/v1/users/99")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.id").value(99));
    }

    @Test
    void getUserById_WithNonNumericPathVariable_ShouldReturn400BadRequest() throws Exception {
        UserResponse adminUser = UserResponse.builder()
                .id(1L)
                .email("admin@hola.edu.vn")
                .roles(Set.of(RoleName.USER, RoleName.ADMIN))
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtTokenService.generateAccessToken(adminUser);

        mockMvc.perform(get("/api/v1/users/abc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9001))
                .andExpect(jsonPath("$.message").value("Tham số 'id' không đúng định dạng dữ liệu"));
    }

    @Test
    void getUserById_WithNegativeId_ShouldReturn400HandlerMethodValidation() throws Exception {
        UserResponse adminUser = UserResponse.builder()
                .id(1L)
                .email("admin@hola.edu.vn")
                .roles(Set.of(RoleName.USER, RoleName.ADMIN))
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtTokenService.generateAccessToken(adminUser);

        mockMvc.perform(get("/api/v1/users/-5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9002))
                .andExpect(jsonPath("$.data.id").value("ID phải là số nguyên dương"));
    }

    @Test
    void register_WhenDataIntegrityViolationOccurs_ShouldReturn409Conflict() throws Exception {
        when(authService.register(any())).thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        String requestJson = """
                {
                  "email": "student@hola.edu.vn",
                  "password": "Password123!",
                  "fullName": "Nguyen Van A"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(9005))
                .andExpect(jsonPath("$.message").value("Dữ liệu bị trùng lặp hoặc xung đột với trạng thái hiện tại"));
    }

    @Test
    void login_WithValidCredentials_ShouldReturn200Ok() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(RoleName.USER))
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("jwt-access-token")
                .refreshToken("opaque-refresh-token")
                .expiresIn(3600)
                .user(userResponse)
                .build();

        when(authService.login(any())).thenReturn(authResponse);

        String requestJson = """
                {
                  "email": "student@hola.edu.vn",
                  "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("opaque-refresh-token"))
                .andExpect(jsonPath("$.data.user.email").value("student@hola.edu.vn"));
    }

    @Test
    void login_WithInvalidPayload_ShouldReturn400ValidationError() throws Exception {
        String invalidJson = """
                {
                  "email": "invalid-email",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9002))
                .andExpect(jsonPath("$.data.email").value("Định dạng email không hợp lệ"))
                .andExpect(jsonPath("$.data.password").value("Mật khẩu không được để trống"));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturn401Unauthorized() throws Exception {
        when(authService.login(any())).thenThrow(new AppException(ErrorCode.INVALID_CREDENTIALS));

        String requestJson = """
                {
                  "email": "student@hola.edu.vn",
                  "password": "WrongPassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1004))
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không chính xác"));
    }

    @Test
    void login_WhenAccountTemporarilyLocked_ShouldReturn403ForbiddenWithCode1007() throws Exception {
        when(authService.login(any())).thenThrow(new AppException(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED));

        String requestJson = """
                {
                  "email": "student@hola.edu.vn",
                  "password": "WrongPassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1007))
                .andExpect(jsonPath("$.message").value("Tài khoản tạm thời bị khóa do đăng nhập sai quá nhiều lần, vui lòng thử lại sau 15 phút"));
    }

    @Test
    void refresh_WithValidToken_ShouldReturn200Ok() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(RoleName.USER))
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("new-jwt-access-token")
                .refreshToken("new-opaque-refresh-token")
                .expiresIn(3600)
                .user(userResponse)
                .build();

        when(authService.refresh(any())).thenReturn(authResponse);

        String requestJson = """
                {
                  "refreshToken": "valid-refresh-token"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Làm mới phiên đăng nhập thành công"))
                .andExpect(jsonPath("$.data.accessToken").value("new-jwt-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-opaque-refresh-token"));
    }

    @Test
    void refresh_WithBlankToken_ShouldReturn400ValidationError() throws Exception {
        String invalidJson = """
                {
                  "refreshToken": "   "
                }
                """;

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9002))
                .andExpect(jsonPath("$.data.refreshToken").value("Refresh token không được để trống"));
    }

    @Test
    void refresh_WithExpiredOrRevokedToken_ShouldReturn401Unauthorized() throws Exception {
        when(authService.refresh(any())).thenThrow(new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

        String requestJson = """
                {
                  "refreshToken": "expired-refresh-token"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1005))
                .andExpect(jsonPath("$.message").value("Phiên làm mới không hợp lệ hoặc đã hết hạn, vui lòng đăng nhập lại"));
    }

    @Test
    void logout_WithValidToken_ShouldReturn200Ok() throws Exception {
        doNothing().when(authService).logout(any());

        String requestJson = """
                {
                  "refreshToken": "valid-refresh-token"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

        verify(authService).logout(any());
    }

    @Test
    void logout_WithBlankToken_ShouldReturn400ValidationError() throws Exception {
        String invalidJson = """
                {
                  "refreshToken": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9002))
                .andExpect(jsonPath("$.data.refreshToken").value("Refresh token không được để trống"));
    }
}

