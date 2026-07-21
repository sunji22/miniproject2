package com.mycom.myapp.user.dto;

import com.mycom.myapp.user.entity.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회원가입 "요청" DTO (클라이언트 -> 서버)
// 컨트롤러에서 @Valid 로 검사 -> 실패 시 MethodArgumentNotValidException -> 400
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {

    // null/빈문자/공백만 불가
    @NotBlank(message = "이메일은 필수입니다.")
    // 이메일 형식 검증
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    // 공백 방지
    @NotBlank(message = "비밀번호는 필수입니다.")
    // 최소 8자 (비밀번호 정책)
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    // 요청 DTO -> User 엔티티
    public User toEntity() {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setName(name);
        return user;
    }
}
