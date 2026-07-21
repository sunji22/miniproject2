package com.mycom.myapp.user.dto;

import com.mycom.myapp.user.entity.Role;
import com.mycom.myapp.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원 "응답" DTO (서버 -> 클라이언트)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;

    // User 엔티티 -> 응답 DTO
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
