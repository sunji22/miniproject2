package com.mycom.myapp.common;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

// GlobalExceptionHandler 가 응답하는 예외 처리 응답 객체
// ErrorResponse 의 필드 구성은 Spring Boot 의 기본 포맷과 유사. 사용자 정의 추가 필드 가능
@Getter
@Builder
public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    @Override
    public String toString() {
        return "ErrorResponse [timestamp=" + timestamp + ", status=" + status + ", error=" + error + ", message="
                + message + ", path=" + path + "]";
    }


}
