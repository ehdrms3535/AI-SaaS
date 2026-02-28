package com.example.saas.api.error;

import com.example.saas.common.ApiException;
import com.example.saas.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ 기존: 예약 충돌 409
    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ReservationConflictException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(baseBody(
                "RESERVATION_CONFLICT",
                e.getMessage(),
                req.getRequestURI()
        ));
    }

    // ✅ 기존: 잘못된 요청 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(baseBody(
                "BAD_REQUEST",
                e.getMessage(),
                req.getRequestURI()
        ));
    }

    // ✅ 추가: ErrorCode 기반 예외 (ORG_NOT_SELECTED, INVALID_CREDENTIALS 등)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException e, HttpServletRequest req) {
        ErrorCode code = e.getCode();
        return ResponseEntity.status(code.status).body(baseBody(
                code.error,
                code.message,
                req.getRequestURI()
        ));
    }

    // ✅ 추가: 인증 실패 401
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(AuthenticationException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(baseBody(
                "UNAUTHORIZED",
                "인증이 필요합니다.",
                req.getRequestURI()
        ));
    }

    // ✅ 추가: 권한 없음 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleDenied(AccessDeniedException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseBody(
                "FORBIDDEN",
                "권한이 없습니다.",
                req.getRequestURI()
        ));
    }

    private Map<String, Object> baseBody(String error, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}