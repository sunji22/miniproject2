package com.mycom.myapp.config;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.mycom.myapp.common.ErrorResponse;

import tools.jackson.databind.ObjectMapper; // Spring Boot 4 = Jackson 3 (tools.jackson.*)

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// 미인증(토큰 없음/무효) 요청이 보호 리소스에 접근할 때 401 응답을 만드는 처리
@Component
@RequiredArgsConstructor
public class MyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        // 401 + JSON 헤더
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpServletResponse.SC_UNAUTHORIZED) // 401
                .error("Unauthorized")
                .message("로그인이 필요합니다")
                .path(request.getRequestURI())
                .build();

        // ErrorResponse -> JSON 문자열로 직렬화해 응답
        objectMapper.writeValue(response.getWriter(), body);
    }
}
