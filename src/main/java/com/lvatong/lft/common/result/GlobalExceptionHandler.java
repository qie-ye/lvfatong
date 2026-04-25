package com.lvatong.lft.common.result;

import com.lvatong.lft.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ApiResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", message);
        return ApiResult.error(400, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: {}", e.getParameterName());
        return ApiResult.error(400, "缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Message not readable: {}", e.getMessage());
        return ApiResult.error(400, "请求体格式错误");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ApiResult.error(400, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return ApiResult.error(403, "权限不足");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResult<Void> handleNotFound(NoResourceFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ApiResult.error(404, "资源不存在");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResult<Void> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not allowed: {}", e.getMethod());
        return ApiResult.error(405, "请求方法不支持: " + e.getMethod());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("File too large: {}", e.getMessage());
        return ApiResult.error(400, "上传文件过大，请不要超过20MB");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return ApiResult.error(500, "服务器内部错误");
    }
}
