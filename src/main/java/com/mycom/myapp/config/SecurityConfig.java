package com.mycom.myapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.mycom.myapp.jwt.JwtAuthenticationFilter;
import com.mycom.myapp.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

// Spring Security 설정
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    // 회원가입 시 인코딩, 로그인 시 매칭에 사용
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 로그인(LoginService)에서 인증을 위임할 AuthenticationManager 를 빈으로 노출
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            MyAuthenticationEntryPoint entryPoint) throws Exception {

        return http
                // JWT 방식 -> 폼로그인/basic/csrf/세션 전부 비활성.
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 인가 규칙
                .authorizeHttpRequests(request -> request
                        // 정적 리소스/랜딩 - 로그인 없이 접근
                        .requestMatchers("/", "/index.html", "/assets/**", "/.well-known/**").permitAll()
                        // 에러 디스패치 경로
                        .requestMatchers("/error").permitAll()
                        // Swagger UI / OpenAPI 문서 - 로그인 없이 접근(팀 API 테스트)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // 로그인/토큰 재발급 등 인증 엔드포인트
                        .requestMatchers("/api/auth/**").permitAll()
                        // 회원가입
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        // 정산 - 관리자만 (자금 조작 차단)
                        .requestMatchers(HttpMethod.POST,
                                "/api/settlements/penalty-all",
                                "/api/settlements/refund",
                                "/api/settlements/penalty",
                                "/api/settlements/reward").hasRole("ADMIN")
                        // 참여 관련 조회는 인증 필요 (아래 공개 조회보다 먼저 와야 함)
                        .requestMatchers(HttpMethod.GET, "/api/challenges/my/participations").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/challenges/*/participations").authenticated()
                        // 챌린지 공개 조회(GET) - 목록/상세만
                        .requestMatchers(HttpMethod.GET, "/api/challenges/**").permitAll()
                        // 관리자 전용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 그 외 전부 인증 필요
                        .anyRequest().authenticated())
                // 미인증 요청 -> 401
                .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(entryPoint))
                // JWT 검증 필터를 기본 폼로그인 필터 앞에 배치(직접 만든 필터)
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
