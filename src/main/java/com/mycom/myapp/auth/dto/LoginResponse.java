package com.mycom.myapp.auth.dto;

import com.mycom.myapp.user.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 로그인 "응답" DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken; // 발급된 JWT access 토큰
    private String email;
    private String name;
    private Role role;
}
