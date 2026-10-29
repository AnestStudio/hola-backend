package org.anest.hola.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.anest.hola.common.dto.ApiResponse;
import org.anest.hola.common.i18n.MessageKeys;
import org.anest.hola.common.i18n.MessageUtils;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Comparator<FieldError> FIELD_ERROR_COMPARATOR = Comparator
            .comparing(FieldError::getField)
            .thenComparingInt(GlobalExceptionHandler::constraintPriority)
            .thenComparing(fe -> fe.getCode() != null ? fe.getCode() : "");

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        List<FieldError> sortedErrors = ex.getBindingResult().getFieldErrors().stream()
                .sorted(FIELD_ERROR_COMPARATOR)
                .toList();
        for (FieldError fieldError : sortedErrors) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage(), errors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            MethodParameter parameter = result.getMethodParameter();
            String paramName = parameter.getParameterName() != null
                    ? parameter.getParameterName()
                    : "param" + parameter.getParameterIndex();
            for (MessageSourceResolvable resolvable : result.getResolvableErrors()) {
                if (resolvable instanceof FieldError fieldError) {
                    errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
                } else {
                    errors.putIfAbsent(paramName, resolvable.getDefaultMessage());
                }
            }
        }
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage(), errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().stream()
                .sorted(Comparator.comparing((ConstraintViolation<?> v) -> extractLeafNodeName(v.getPropertyPath().toString()))
                        .thenComparing(v -> v.getMessageTemplate() != null ? v.getMessageTemplate() : ""))
                .forEach(violation -> {
                    String fieldName = extractLeafNodeName(violation.getPropertyPath().toString());
                    errors.putIfAbsent(fieldName, violation.getMessage());
                });
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage(), errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        String message = MessageUtils.get(MessageKeys.ERROR_TYPE_MISMATCH, ex.getName());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        String message = MessageUtils.get(MessageKeys.ERROR_MISSING_PARAMETER, ex.getParameterName());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), message));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), MessageUtils.get(MessageKeys.ERROR_MALFORMED_JSON)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation occurred: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.DATA_CONFLICT;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    private static int constraintPriority(FieldError fieldError) {
        String code = fieldError.getCode();
        if (code == null) {
            return 10;
        }
        return switch (code) {
            case "NotBlank", "NotEmpty", "NotNull" -> 1;
            case "Size", "Length", "Min", "Max", "Positive", "PositiveOrZero" -> 2;
            case "Email", "Pattern" -> 3;
            default -> 5;
        };
    }

    private static String extractLeafNodeName(String propertyPath) {
        if (propertyPath == null || propertyPath.isEmpty()) {
            return "param";
        }
        int lastDot = propertyPath.lastIndexOf('.');
        return (lastDot >= 0 && lastDot < propertyPath.length() - 1)
                ? propertyPath.substring(lastDot + 1)
                : propertyPath;
    }
}
