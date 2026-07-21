package com.mycom.myapp.auth.service;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.mycom.myapp.auth.dto.LoginRequest;
import com.mycom.myapp.auth.dto.LoginResponse;
import com.mycom.myapp.config.MyUserDetails;
import com.mycom.myapp.jwt.JwtUtil;
import com.mycom.myapp.user.entity.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 로그인 서비스 구현
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginServiceImpl implements LoginService {

    private final AuthenticationManager authenticationManager; // SecurityConfig 에서 빈 등록
    private final JwtUtil jwtUtil;

    @Override
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

        // access 토큰 발급
        String accessToken = jwtUtil.createToken(principal.getUsername(), roles);

        Role role = Role.valueOf(roles.getFirst().substring(5));

        log.info("Login success: {}", principal.getEmail());
        return LoginResponse.builder()
                .accessToken(accessToken)
                .email(principal.getEmail())
                .name(principal.getName())
                .role(role)
                .build();
    }
}
