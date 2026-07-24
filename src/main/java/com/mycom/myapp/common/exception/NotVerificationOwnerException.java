package com.mycom.myapp.common.exception;

// 남이 쓴 인증글을 수정하려 할 때 던지는 예외
public class NotVerificationOwnerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotVerificationOwnerException(Long verificationId) {
        super("본인이 작성한 인증글만 수정할 수 있습니다. id=" + verificationId);
    }
}
