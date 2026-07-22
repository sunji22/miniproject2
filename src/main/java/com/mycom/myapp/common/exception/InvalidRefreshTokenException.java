package com.mycom.myapp.common.exception;

// Refresh 토큰 재발급 시 토큰이 유효하지 않을 때 던지는 예외
public class InvalidRefreshTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidRefreshTokenException() {
        super("유효하지 않은 리프레시 토큰입니다.");
    }
}
