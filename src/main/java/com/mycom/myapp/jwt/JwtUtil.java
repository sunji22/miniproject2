package com.mycom.myapp.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.mycom.myapp.config.MyUserDetailsService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// JWT 생성/검증 유틸
@Component
@RequiredArgsConstructor
@Getter
@Slf4j
public class JwtUtil {

    private final MyUserDetailsService myUserDetailsService;

    @Value("${myapp.jwt.secret}")
    private String secretKeyStr;

    @Value("${myapp.jwt.access-exp}")
    private long accessTokenValidMs;   // access 토큰 유효기간

    @Value("${myapp.jwt.refresh-exp}")
    private long refreshTokenValidMs;  // refresh 토큰 유효기간

    private SecretKey secretKey;

    // 빈 생성 직후 1회: 문자열 secret -> SecretKey 객체로 변환
    @PostConstruct
    protected void init() {
        secretKey = new SecretKeySpec(
                secretKeyStr.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm()
        );
    }

    // Access 토큰 생성
    public String createToken(String username, List<String> roles) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)                 // sub
                .claim("roles", roles)             // 권한 목록(공개 노출되므로 민감정보 X)
                .issuedAt(now)                     // iat
                .expiration(new Date(now.getTime() + accessTokenValidMs)) // exp
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    // Refresh 토큰 생성
    public String createRefreshToken(String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenValidMs))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    // 토큰 -> 사용자 식별자(sub) 추출
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // 프론트가 X-AUTH-TOKEN 헤더에 담아 보낸 토큰 추출
    // 없으면 null
    public String getTokenFromHeader(HttpServletRequest request) {
        return request.getHeader("X-AUTH-TOKEN");
    }

    // 서명/만료 검증
    // 유효하면 Claims, 아니면 null
    public Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 만료 시각이 현재보다 과거면 만료
            if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
                return null;
            }
            return claims;
        } catch (Exception e) {
            // 서명 불일치/형식 오류 등 모든 예외 -> 무효 토큰 취급
            return null;
        }
    }

    // DB 2차 검증: 토큰의 username 으로 최신 UserDetails 를 DB 에서 조회해 Authentication 생성.
    // (탈퇴/권한변경이 즉시 반영됨 - 방식 2, 비관적 검증)
    public UsernamePasswordAuthenticationToken getAuthentication(String token) {
        UserDetails userDetails = myUserDetailsService.loadUserByUsername(getUsernameFromToken(token));
        return new UsernamePasswordAuthenticationToken(
                userDetails.getUsername(),
                "",                          // 자격증명(비밀번호)은 이미 검증됨 → 비움
                userDetails.getAuthorities());
    }
}
