package com.mycom.myapp.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 매 요청마다 JWT 를 검사해 인증정보를 SecurityContext 에 주입하는 필터
// [흐름] 헤더에서 토큰 추출 -> 서명/만료 검증 -> 유효하면 DB 2차 검증으로 Authentication 생성 -> SecurityContextHolder 에 저장
// 토큰이 없거나 무효면 아무것도 안 하고 통과
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 요청에서 JWT 추출(없으면 null)
        String token = jwtUtil.getTokenFromHeader(request);

        // 서명/만료 검증 토큰 없으면 검증 생략
        Claims claims = (token != null) ? jwtUtil.validateToken(token) : null;

        //  유효 토큰이면 DB 2차 검증 후 인증정보 주입
        if (claims != null) {
            UsernamePasswordAuthenticationToken authenticationToken = jwtUtil.getAuthentication(token);

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // 세션이 아니라 이 요청의 SecurityContext(필터체인 동안 공유되는 공간)에 저장
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}
