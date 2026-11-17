package com.wayfarer.travel.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);

    public record ErrorBody(String code, String message, boolean retryable) {
        public static ErrorBody from(ApiFailure failure) {
            return new ErrorBody(failure.code(), failure.getMessage(), failure.retryable());
        }
    }

    @ExceptionHandler(ApiFailure.class)
    ResponseEntity<ErrorBody> known(ApiFailure failure) {
        return ResponseEntity.status(failure.status()).body(ErrorBody.from(failure));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException exception) {
        var field = exception.getBindingResult().getFieldError();
        String message = field == null ? "请检查表单内容。" : field.getDefaultMessage();
        return known(ApiFailure.invalid(message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorBody> malformed() {
        return known(ApiFailure.invalid("请求格式有误，请检查填写的内容。"));
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    ResponseEntity<ErrorBody> database(org.springframework.dao.DataAccessException failure) {
        LOG.error("Database operation failed: {}", failure.getClass().getSimpleName());
        return known(new ApiFailure(503, "DATABASE_UNAVAILABLE", "暂时无法读取或保存数据，请稍后重试。", true));
    }
}
