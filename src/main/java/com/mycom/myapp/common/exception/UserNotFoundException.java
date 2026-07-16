package com.mycom.myapp.common.exception;

// 회원(user)을 userId 로 조회했는데 없을 때 던지는 예외
// [주의]
// Spring Security 의 UsernameNotFoundException => 인증 실패로 401
// 이 예외는 인증된 뒤, 특정 회원 리소스를 조회했는데 없는 상황 => 404
public class UserNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException(Long userId) {
        super("회원을 찾을 수 없습니다. id=" + userId);
    }
}
