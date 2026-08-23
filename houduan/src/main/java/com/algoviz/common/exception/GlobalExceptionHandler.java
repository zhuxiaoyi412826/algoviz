package com.algoviz.common.exception;

import com.algoviz.common.enums.ErrorCode;
import com.algoviz.dto.interview.InterviewResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一捕获所有异常，转换为标准 InterviewResponse 格式返回给前端
 *
 * 使用：在 Controller/Service 层直接 throw BusinessException/UnauthorizedException 等，
 *       无需在每个方法里 try/catch 再包装返回。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 自定义业务异常 ====================

    /**
     * 业务异常：正常业务逻辑错误（密码错误、金币不足等）
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public InterviewResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("[BusinessException] code={}, message={}", e.getCode(), e.getMessage());
        return InterviewResponse.fail(e.getCode(), e.getMessage());
    }

    /**
     * 未认证异常（401）
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public InterviewResponse<Void> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("[UnauthorizedException] code={}, message={}", e.getCode(), e.getMessage());
        return InterviewResponse.fail(e.getCode(), e.getMessage());
    }

    /**
     * 无权限异常（403）
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public InterviewResponse<Void> handleForbiddenException(ForbiddenException e) {
        log.warn("[ForbiddenException] code={}, message={}", e.getCode(), e.getMessage());
        return InterviewResponse.fail(e.getCode(), e.getMessage());
    }

    // ==================== 参数校验异常 ====================

    /**
     * @Valid @RequestBody 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public InterviewResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[MethodArgumentNotValid] {}", msg);
        return InterviewResponse.fail(ErrorCode.VALIDATION_FAILED.getCode(), msg);
    }

    /**
     * @Valid @ModelAttribute / GET 参数绑定校验失败
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public InterviewResponse<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[BindException] {}", msg);
        return InterviewResponse.fail(ErrorCode.VALIDATION_FAILED.getCode(), msg);
    }

    /**
     * 路径变量 / RequestParam 上的 @Validated 校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public InterviewResponse<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("[ConstraintViolation] {}", msg);
        return InterviewResponse.fail(ErrorCode.VALIDATION_FAILED.getCode(), msg);
    }

    /**
     * 缺少必填 @RequestParam 参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public InterviewResponse<Void> handleMissingParamException(MissingServletRequestParameterException e) {
        log.warn("[MissingParam] 参数名={}, 类型={}", e.getParameterName(), e.getParameterType());
        return InterviewResponse.fail(ErrorCode.MISSING_PARAMETER.getCode(),
                "缺少必填参数: " + e.getParameterName());
    }

    /**
     * 参数类型不匹配（如把字符串传给 Integer 类型的 id）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public InterviewResponse<Void> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("[TypeMismatch] 参数名={}, 期望类型={}, 实际值={}",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "?", e.getValue());
        return InterviewResponse.fail(ErrorCode.PARAM_TYPE_MISMATCH.getCode(),
                "参数 " + e.getName() + " 类型错误");
    }

    /**
     * 请求体 JSON 解析失败（格式错误 / 字段类型不匹配）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public InterviewResponse<Void> handleMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("[HttpMessageNotReadable] {}", e.getMessage());
        return InterviewResponse.fail(ErrorCode.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    // ==================== HTTP 协议错误 ====================

    /**
     * 请求路径不存在（404），返回 JSON 而不是 HTML
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public InterviewResponse<Void> handleNotFoundException(Exception e) {
        log.warn("[NotFound] {}", e.getMessage());
        return InterviewResponse.fail(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage());
    }

    /**
     * 请求方法不支持（POST 写成 GET 等）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public InterviewResponse<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("[MethodNotSupported] {} 不支持，期望: {}", e.getMethod(), e.getSupportedHttpMethods());
        return InterviewResponse.fail(405, "请求方法 " + e.getMethod() + " 不允许");
    }

    /**
     * Content-Type 不支持
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public InterviewResponse<Void> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("[MediaTypeNotSupported] 收到: {}", e.getContentType());
        return InterviewResponse.fail(415, "不支持的 Content-Type");
    }

    /**
     * 文件上传超过限制（spring.servlet.multipart.max-file-size）
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public InterviewResponse<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("[FileTooLarge] {}", e.getMessage());
        return InterviewResponse.fail(ErrorCode.FILE_TOO_LARGE.getCode(), ErrorCode.FILE_TOO_LARGE.getMessage());
    }

    // ==================== 其它系统异常 ====================

    /**
     * 数据库异常（脱敏返回，避免泄露 SQL / 表结构）
     */
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public InterviewResponse<Void> handleSQLException(SQLException e) {
        log.error("[SQLException] SQL状态={}, 错误码={}, message={}",
                e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
        return InterviewResponse.fail(ErrorCode.DATABASE_ERROR.getCode(), ErrorCode.DATABASE_ERROR.getMessage());
    }

    /**
     * 兜底异常：所有未捕获的异常统一返回 500，不暴露堆栈给前端
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public InterviewResponse<Void> handleException(Exception e) {
        log.error("[UnhandledException] type={}, message={}", e.getClass().getName(), e.getMessage(), e);
        return InterviewResponse.fail(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
    }
}
