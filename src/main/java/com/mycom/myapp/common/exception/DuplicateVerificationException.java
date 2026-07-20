package com.mycom.myapp.common.exception;

// 같은 참여(participation)에 대해 같은 날 인증글을 두 번 올리려 할 때 던지는 예외
// DB 의 UNIQUE(participation_id, verified_date) 제약과 대응 (하루 1인증).
public class DuplicateVerificationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateVerificationException() {
        super("오늘 이미 인증했습니다.");
    }
}
