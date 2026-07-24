package com.mycom.myapp.common.exception;

// 인증글 목록 조회 시 challengeId / participationId 를 둘 다 주지 않았을 때 던지는 예외
public class MissingVerificationFilterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MissingVerificationFilterException() {
        super("challengeId 또는 participationId 중 하나는 필수입니다.");
    }
}
