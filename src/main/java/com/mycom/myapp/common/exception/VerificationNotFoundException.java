package com.mycom.myapp.common.exception;

// 조회하려는 인증글이 DB 에 없을 때 던지는 예외
public class VerificationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VerificationNotFoundException(Long verificationId) {
        super("인증글을 찾을 수 없습니다. id=" + verificationId);
    }
}
