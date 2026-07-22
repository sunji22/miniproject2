package com.mycom.myapp.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.auth.dto.LoginRequest;
import com.mycom.myapp.auth.dto.LoginResponse;
import com.mycom.myapp.auth.dto.TokenRefreshRequest;
import com.mycom.myapp.auth.dto.TokenResponse;
import com.mycom.myapp.auth.entity.RefreshToken;
import com.mycom.myapp.auth.repository.RefreshTokenRepository;
import com.mycom.myapp.common.exception.InvalidRefreshTokenException;
import com.mycom.myapp.common.exception.UserNotFoundException;
import com.mycom.myapp.config.MyUserDetails;
import com.mycom.myapp.jwt.JwtUtil;
import com.mycom.myapp.user.entity.Role;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 로그인 서비스 구현
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginServiceImpl implements LoginService {

    private final AuthenticationManager authenticationManager; // SecurityConfig 에서 빈 등록
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;               // refresh 저장 시 User 참조 확보
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest req) {

        // 미인증 토큰(email,password)을 매니저에게 위임
        // 성공 시 인증된 Authentication 반환, 실패시 예외
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        // 인증 성공
        MyUserDetails principal = (MyUserDetails) auth.getPrincipal();

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // access + refresh 토큰 발급
        String accessToken = jwtUtil.createToken(principal.getUsername(), roles);
        String refreshToken = jwtUtil.createRefreshToken(principal.getUsername());

        // refresh 는 DB 에도 저장(user 1:1 upsert) -> 재발급 때 이 값과 대조
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException(principal.getId()));
        upsertRefreshToken(user, refreshToken);

        Role role = Role.valueOf(roles.getFirst().substring(5));

        log.info("Login success: {}", principal.getEmail());
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(principal.getEmail())
                .name(principal.getName())
                .role(role)
                .build();
    }

    @Override
    @Transactional
    public TokenResponse reissue(TokenRefreshRequest req) {
        String requestToken = req.getRefreshToken();

        // JWT 자체 검증(서명/만료)
        Claims claims = jwtUtil.validateToken(requestToken);
        if (claims == null) {
            throw new InvalidRefreshTokenException();
        }

        String email = claims.getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidRefreshTokenException::new);

        RefreshToken saved = refreshTokenRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!saved.getToken().equals(requestToken)
                || saved.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException();
        }

        // access + refresh 모두 새로 발급하고 DB 행을 덮어씀
        List<String> roles = List.of(user.getRole().authority());
        String newAccess = jwtUtil.createToken(email, roles);
        String newRefresh = jwtUtil.createRefreshToken(email);

        saved.setToken(newRefresh);
        saved.setExpiresAt(refreshExpiresAt());
        // @Transactional dirty checking 으로 UPDATE 되지만, 의도를 드러내기 위해 명시 save
        refreshTokenRepository.save(saved);

        log.info("Token reissued: {}", email);
        return TokenResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .build();
    }

    // user 있으면 token/만료 갱신(덮어쓰기), 없으면 새로 저장
    private void upsertRefreshToken(User user, String refreshToken) {
        LocalDateTime expiresAt = refreshExpiresAt();
        refreshTokenRepository.findByUser_UserId(user.getUserId())
                .ifPresentOrElse(
                        existing -> {
                            existing.setToken(refreshToken);
                            existing.setExpiresAt(expiresAt);
                        },
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .user(user)
                                        .token(refreshToken)
                                        .expiresAt(expiresAt)
                                        .build()));
    }

    // refresh 유효기간을 현재시각에 더해 만료시각 계산
    private LocalDateTime refreshExpiresAt() {
        return LocalDateTime.now().plus(Duration.ofMillis(jwtUtil.getRefreshTokenValidMs()));
    }
}
