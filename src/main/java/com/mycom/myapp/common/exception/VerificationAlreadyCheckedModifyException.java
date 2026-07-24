package com.mycom.myapp.common.exception;

// 이미 상호체크를 받은 인증글을 수정하려 할 때 던지는 예외
public class VerificationAlreadyCheckedModifyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VerificationAlreadyCheckedModifyException(Long verificationId) {
        super("이미 상호체크를 받은 인증글은 수정할 수 없습니다. id=" + verificationId);
    }
}
