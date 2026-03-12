package com.example.saas.api.error;

import com.example.saas.common.ApiException;
import com.example.saas.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
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

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequestException(BadRequestException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(baseBody(
                e.getError(),
                e.getMessage(),
                req.getRequestURI()
        ));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(baseBody(
                e.getError(),
                e.getMessage(),
                req.getRequestURI()
        ));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflictException(ConflictException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(baseBody(
                e.getError(),
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

    // ✅ 추가: 요청 바디 파싱 실패 (잘못된 JSON 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException e, HttpServletRequest req) {
        String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(baseBody(
                "BAD_REQUEST",
                msg,
                req.getRequestURI()
        ));
    }

    // ✅ 추가: @Valid 유효성 검사 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<String> errors = new ArrayList<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.add(fe.getField() + ": " + fe.getDefaultMessage());
        }
        String message = String.join("; ", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(baseBody(
                "VALIDATION_FAILED",
                message.isEmpty() ? "유효성 검사 실패" : message,
                req.getRequestURI()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest req) {
        String message = "요청 데이터가 유효하지 않습니다.";
        Throwable root = e.getMostSpecificCause();
        if (root != null && root.getMessage() != null) {
            String rootMessage = root.getMessage();
            if (rootMessage.contains("reservations_customer_id_fkey")) {
                message = "고객을 찾을 수 없습니다.";
            } else if (rootMessage.contains("reservations_service_id_fkey")) {
                message = "서비스를 찾을 수 없습니다.";
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(baseBody(
                "DATA_INTEGRITY_VIOLATION",
                message,
                req.getRequestURI()
        ));
    }

    // 개발용: 모든 예외를 잡아 상세한 스택트레이스를 응답에 포함 (운영환경에서는 비활성화 권장)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception e, HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement st : e.getStackTrace()) {
            sb.append("    at ").append(st.toString()).append("\n");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(baseBody(
                "INTERNAL_ERROR",
                sb.toString(),
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
