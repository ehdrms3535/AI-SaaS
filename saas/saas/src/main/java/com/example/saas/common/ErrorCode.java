package com.example.saas.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다."),
    ORG_NOT_SELECTED(HttpStatus.BAD_REQUEST, "ORG_NOT_SELECTED", "조직이 선택되지 않았습니다. X-ORG-ID 헤더를 지정하세요."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "리프레시 토큰이 유효하지 않습니다."),
    REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REVOKED", "리프레시 토큰이 폐기되었습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    EMAIL_TAKEN(HttpStatus.BAD_REQUEST, "EMAIL_TAKEN", "이미 사용 중인 이메일입니다."),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "EMAIL_NOT_FOUND", "가입된 이메일을 찾을 수 없습니다."),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_INVALID", "비밀번호 재설정 토큰이 유효하지 않습니다."),
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_EXPIRED", "비밀번호 재설정 토큰이 만료되었습니다."),
    PLAN_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "PLAN_LIMIT_EXCEEDED", "현재 플랜 한도를 초과했습니다.");

    public final HttpStatus status;
    public final String error;
    public final String message;

    ErrorCode(HttpStatus status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }
}
