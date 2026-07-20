package com.mycom.myapp.common.exception;

// 회원가입 시 이미 가입된 이메일로 다시 가입하려 할 때 던지는 예외
// DB 의 UNIQUE(email) 제약과 대응. 서비스에서 findByEmail 로 선검사 후 이 예외를 던짐
public class EmailAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailAlreadyExistsException(String email) {
        super("이미 사용 중인 이메일입니다. email=" + email);
    }
}
