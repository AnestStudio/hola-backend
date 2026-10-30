package org.anest.hola.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.anest.hola.common.i18n.MessageKeys;
import org.anest.hola.common.i18n.MessageUtils;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // System & Common Errors (9xxx)
    INTERNAL_SERVER_ERROR(9999, MessageKeys.ERROR_INTERNAL_SERVER, HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(9001, MessageKeys.ERROR_INVALID_REQUEST, HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(9002, MessageKeys.ERROR_VALIDATION_FAILED, HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(9003, MessageKeys.ERROR_RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(9004, MessageKeys.ERROR_METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED),
    DATA_CONFLICT(9005, MessageKeys.ERROR_DATA_CONFLICT, HttpStatus.CONFLICT),

    // Authentication & Authorization Errors (1xxx)
    UNAUTHENTICATED(1001, MessageKeys.ERROR_UNAUTHENTICATED, HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1002, MessageKeys.ERROR_ACCESS_DENIED, HttpStatus.FORBIDDEN),
    INVALID_TOKEN(1003, MessageKeys.ERROR_INVALID_TOKEN, HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(1004, MessageKeys.ERROR_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(1005, MessageKeys.ERROR_INVALID_REFRESH_TOKEN, HttpStatus.UNAUTHORIZED),
    ACCOUNT_INACTIVE(1006, MessageKeys.ERROR_ACCOUNT_INACTIVE, HttpStatus.FORBIDDEN),
    ACCOUNT_TEMPORARILY_LOCKED(1007, MessageKeys.ERROR_ACCOUNT_TEMPORARILY_LOCKED, HttpStatus.FORBIDDEN),
    ACCOUNT_UNVERIFIED(1008, MessageKeys.ERROR_ACCOUNT_UNVERIFIED, HttpStatus.FORBIDDEN),
    INVALID_OTP(1009, MessageKeys.ERROR_INVALID_OTP, HttpStatus.BAD_REQUEST),
    OTP_MAX_ATTEMPTS_EXCEEDED(1010, MessageKeys.ERROR_OTP_MAX_ATTEMPTS_EXCEEDED, HttpStatus.BAD_REQUEST),
    ACCOUNT_ALREADY_VERIFIED(1011, MessageKeys.ERROR_ACCOUNT_ALREADY_VERIFIED, HttpStatus.CONFLICT),

    // User Domain Errors (2xxx)
    USER_NOT_FOUND(2001, MessageKeys.ERROR_USER_NOT_FOUND, HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(2002, MessageKeys.ERROR_USER_ALREADY_EXISTS, HttpStatus.CONFLICT);

    private final int code;
    private final String messageKey;
    private final HttpStatus httpStatus;

    public String getMessage() {
        return MessageUtils.get(messageKey);
    }
}
