package com.mycom.myapp.common.exception;

// 자기가 쓴 인증글을 자기가 상호체크하려 할 때 던지는 예외
public class SelfCheckNotAllowedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SelfCheckNotAllowedException() {
        super("본인이 작성한 인증글은 체크할 수 없습니다.");
    }
}
