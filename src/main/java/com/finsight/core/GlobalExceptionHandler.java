package com.finsight.core;

import com.finsight.domain.common.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;

@org.springframework.web.bind.annotation.RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource){
        this.messageSource = messageSource;
    }

    @ExceptionHandler(value = Exception.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity catchExceptionHandler(Exception exception) {
        log.error(exception.getMessage(), exception);

        String message;
        Throwable tempEx = getCause(exception);
        String lower = (exception == null || exception.getMessage() == null) ? "" : exception.getMessage().toLowerCase();
        Throwable walker = exception;
        while(walker != null){
            String m = walker.getMessage();
            if(m != null){ lower = lower + " " + m.toLowerCase(); }
            walker = walker.getCause();
        }
        if (tempEx instanceof AppException) {
            AppException appEx = (AppException) tempEx;
            message = messageSource.getMessage(appEx.getDescription(), appEx.getParameters(), appEx.getMessage(), Locale.getDefault());
            return ResponseEntity.error(message);
        }

        if (tempEx instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) tempEx;
            if (Objects.nonNull(ex)) {
                BindingResult bindingResult = ex.getBindingResult();
                if (Objects.nonNull(bindingResult)) {
                    FieldError fieldError = bindingResult.getFieldError();
                    if (Objects.nonNull(fieldError)) {
                        message = fieldError.getDefaultMessage();
                        return ResponseEntity.error(message);
                    }
                }
            }
        }
        
        if (tempEx instanceof TypeMismatchException) {
            TypeMismatchException ex = (TypeMismatchException) tempEx;
            String propertyName = ex.getPropertyName();
            Class<?> requiredType = ex.getRequiredType();
            String typeName = requiredType != null ? requiredType.getSimpleName() : "unknown";
            
            if ("Date".equalsIgnoreCase(typeName)) {
                message = String.format("日期格式错误: 参数 [%s] 必须是有效的日期格式", propertyName);
            } else if ("Integer".equalsIgnoreCase(typeName) || "Long".equalsIgnoreCase(typeName) || "Double".equalsIgnoreCase(typeName)) {
                message = String.format("数字格式错误: 参数 [%s] 必须是有效的数字", propertyName);
            } else {
                message = String.format("参数格式错误: 参数 [%s] 类型应为 %s", propertyName, typeName);
            }
            return ResponseEntity.error(message);
        }

        if (exception instanceof DataIntegrityViolationException){

        }

        if (exception instanceof org.springframework.jdbc.CannotGetJdbcConnectionException
                || lower.contains("cannot get jdbc connection")
                || lower.contains("communications link failure")
                || lower.contains("failed to obtain jdbc connection")
                || (lower.contains("jdbc") && lower.contains("connect"))) {
            message = "数据库连接失败，请确认数据库服务已启动，并检查网络与配置。";
            return ResponseEntity.error(message);
        }
        if (lower.contains("access denied for user") || lower.contains("authentication")){
            message = "数据库认证失败，请检查数据库用户名和密码是否正确。";
            return ResponseEntity.error(message);
        }
        if (lower.contains("unknown database")){
            message = "数据库不存在，请检查数据库名称配置。";
            return ResponseEntity.error(message);
        }

        message = messageSource.getMessage("error.system.error", null, "System error", Locale.getDefault());
        return ResponseEntity.error(message);
    }

    private Throwable getCause(Throwable ex) {
        if (ex instanceof AppException) {
            return ex;
        } else {
            if (ex.getCause() == null) {
                return ex;
            } else {
                return getCause(ex.getCause());
            }
        }
    }
}
