package org.anest.hola.common.i18n;

public final class MessageKeys {

    private MessageKeys() {
    }

    // Common & System
    public static final String COMMON_SUCCESS = "common.success";
    public static final String ERROR_INTERNAL_SERVER = "error.internal_server_error";
    public static final String ERROR_INVALID_REQUEST = "error.invalid_request";
    public static final String ERROR_TYPE_MISMATCH = "error.type_mismatch";
    public static final String ERROR_MISSING_PARAMETER = "error.missing_parameter";
    public static final String ERROR_MALFORMED_JSON = "error.malformed_json";
    public static final String ERROR_VALIDATION_FAILED = "error.validation_failed";
    public static final String ERROR_RESOURCE_NOT_FOUND = "error.resource_not_found";
    public static final String ERROR_METHOD_NOT_ALLOWED = "error.method_not_allowed";
    public static final String ERROR_DATA_CONFLICT = "error.data_conflict";

    // Auth & Security
    public static final String ERROR_UNAUTHENTICATED = "error.unauthenticated";
    public static final String ERROR_ACCESS_DENIED = "error.access_denied";
    public static final String ERROR_INVALID_TOKEN = "error.invalid_token";
    public static final String ERROR_INVALID_CREDENTIALS = "error.invalid_credentials";
    public static final String ERROR_INVALID_REFRESH_TOKEN = "error.invalid_refresh_token";
    public static final String ERROR_ACCOUNT_INACTIVE = "error.account_inactive";
    public static final String ERROR_ACCOUNT_TEMPORARILY_LOCKED = "error.account_temporarily_locked";
    public static final String ERROR_ACCOUNT_UNVERIFIED = "error.account_unverified";
    public static final String ERROR_INVALID_OTP = "error.invalid_otp";
    public static final String ERROR_OTP_MAX_ATTEMPTS_EXCEEDED = "error.otp_max_attempts_exceeded";
    public static final String ERROR_ACCOUNT_ALREADY_VERIFIED = "error.account_already_verified";

    public static final String AUTH_REGISTER_SUCCESS = "auth.register.success";
    public static final String AUTH_VERIFY_EMAIL_SUCCESS = "auth.verify_email.success";
    public static final String AUTH_RESEND_OTP_SUCCESS = "auth.resend_otp.success";
    public static final String AUTH_FORGOT_PASSWORD_SUCCESS = "auth.forgot_password.success";
    public static final String AUTH_RESET_PASSWORD_SUCCESS = "auth.reset_password.success";
    public static final String AUTH_LOGIN_SUCCESS = "auth.login.success";
    public static final String AUTH_REFRESH_SUCCESS = "auth.refresh.success";
    public static final String AUTH_LOGOUT_SUCCESS = "auth.logout.success";

    // User
    public static final String ERROR_USER_NOT_FOUND = "error.user_not_found";
    public static final String ERROR_USER_ALREADY_EXISTS = "error.user_already_exists";
}

