package com.mycom.myapp.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.common.exception.EmailAlreadyExistsException;
import com.mycom.myapp.user.dto.UserCreateRequest;
import com.mycom.myapp.user.dto.UserResponse;
import com.mycom.myapp.user.entity.Role;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 회원가입
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    // SecurityConfig 의 PasswordEncoder(BCrypt) 빈을 주입받아 사용
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse register(UserCreateRequest req) {

        // 이메일 중복 검사
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new EmailAlreadyExistsException(req.getEmail());
        }

        // 요청 DTO -> 엔티티 (email/password(평문)/name 매핑)
        User user = req.toEntity();

        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        User savedUser = userRepository.save(user);

        // 응답 DTO 로 변환(비밀번호 제외)
        return UserResponse.fromEntity(savedUser);
    }
}
