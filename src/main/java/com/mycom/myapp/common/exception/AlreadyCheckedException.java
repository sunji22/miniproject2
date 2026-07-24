package com.mycom.myapp.common.exception;

// 같은 인증글을 같은 회원이 두 번 체크하려 할 때 던지는 예외
public class AlreadyCheckedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AlreadyCheckedException() {
        super("이미 체크한 인증글입니다.");
    }
}
