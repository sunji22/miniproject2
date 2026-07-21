package com.mycom.myapp.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;

// JwtUtil 순수 단위 테스트
class JwtUtilTest {

    // 테스트 전용 더미 시크릿
    private static final String SECRET = "test-secret-key-for-jwtutil-unit-1234567890";
    private static final long ACCESS_MS = 1000L * 60 * 60;      // 1시간
    private static final long REFRESH_MS = 1000L * 60 * 60 * 24; // 24시간

    private static final String EMAIL = "user@test.com";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // service 는 이 테스트에서 안 쓰므로 null (getAuthentication 미검증)
        jwtUtil = new JwtUtil(null);

        // private @Value 필드 수동 주입
        ReflectionTestUtils.setField(jwtUtil, "secretKeyStr", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenValidMs", ACCESS_MS);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenValidMs", REFRESH_MS);

        // @PostConstruct init() 수동 호출 -> secretKey 생성
        ReflectionTestUtils.invokeMethod(jwtUtil, "init");
    }

    // 발급한 access 토큰이 유효한가
    // 비교: 검증 claims != null, sub == EMAIL, roles 에 "ROLE_USER" 포함 -> 셋 다 맞으면 통과
    @Test
    @DisplayName("createToken -> validateToken : 유효 토큰, sub=email, roles 클레임 포함")
    void createAndValidateAccessToken() {
        // given : access 토큰 생성
        String token = jwtUtil.createToken(EMAIL, List.of("ROLE_USER"));

        // when : 검증
        Claims claims = jwtUtil.validateToken(token);

        // then : 유효 + 클레임 확인
        assertNotNull(claims);
        assertEquals(EMAIL, claims.getSubject());

        // roles 클레임은 List 로 직렬화됨
        assertTrue(claims.get("roles", List.class).contains("ROLE_USER"));
    }

    // 토큰에서 꺼낸 사용자 식별자 확인
    // 비교: getUsernameFromToken 결과 == 넣은 EMAIL -> 같으면 통과
    @Test
    @DisplayName("getUsernameFromToken : 토큰에서 sub(email) 추출")
    void getUsernameFromToken() {
        String token = jwtUtil.createToken(EMAIL, List.of("ROLE_USER"));

        String username = jwtUtil.getUsernameFromToken(token);

        assertEquals(EMAIL, username);
    }

    // 만료된 토큰은 무효 처리되는가
    // 비교: validateToken 결과 == null -> null 이면 통과
    @Test
    @DisplayName("만료 토큰 : validateToken -> null (만료 분기)")
    void validateExpiredToken() {
        // given : 유효기간을 과거(-1000ms)로 만들어 이미 만료된 토큰 발급
        ReflectionTestUtils.setField(jwtUtil, "accessTokenValidMs", -1000L);
        String expired = jwtUtil.createToken(EMAIL, List.of("ROLE_USER"));

        // when / then : 만료 -> null
        assertNull(jwtUtil.validateToken(expired));
    }

    // 형식/서명 깨진 토큰은 무효 처리되는가
    // 비교: validateToken 결과 == null -> null 이면 통과
    @Test
    @DisplayName("위변조/형식오류 토큰: validateToken -> null (catch 분기)")
    void validateGarbageToken() {
        // 서명 검증 실패/파싱 오류 → 예외를 잡아 null 반환
        assertNull(jwtUtil.validateToken("this.is.garbage"));
    }

    // refresh 토큰 유효 & 권한정보 없음 확인
    // 비교: claims != null, sub == EMAIL, roles 클레임 == null -> 다 맞으면 통과
    @Test
    @DisplayName("createRefreshToken -> validateToken : 유효, sub=email, roles 클레임 없음")
    void createAndValidateRefreshToken() {
        // given : refresh 토큰 생성 (roles 없이 sub+만료만)
        String refresh = jwtUtil.createRefreshToken(EMAIL);

        // when : 검증
        Claims claims = jwtUtil.validateToken(refresh);

        // then : 유효 + sub 확인 + roles 클레임 부재
        assertNotNull(claims);
        assertEquals(EMAIL, claims.getSubject());
        assertNull(claims.get("roles"));
    }
}
