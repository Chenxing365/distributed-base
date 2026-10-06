package com.distributed.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * <p>统一捕获运行时异常，返回标准 JSON 错误响应，
 * 避免 Spring 默认错误页泄露堆栈信息。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理所有未捕获的运行时异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("[GlobalExceptionHandler] 未处理异常: {}", e.getMessage(), e);

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", e.getClass().getSimpleName());
        body.put("message", e.getMessage());
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[GlobalExceptionHandler] 非法参数: {}", e.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "IllegalArgument");
        body.put("message", e.getMessage());
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
