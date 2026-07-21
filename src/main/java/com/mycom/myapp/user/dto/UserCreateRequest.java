package com.mycom.myapp.user.dto;

import com.mycom.myapp.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회원가입 "요청" DTO (클라이언트 -> 서버)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {

    private String email;
    private String password;
    private String name;

    // 요청 DTO -> User 엔티티
    // role/pointBalance/id/createdAt 은 넣지 않음 -> (role 은 서비스가 Role.USER 기본값 설정, 나머지는 JPA/DB 가 채움)
    public User toEntity() {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setName(name);
        return user;
    }
}
