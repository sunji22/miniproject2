package com.mycom.myapp.common.exception;

/**
 * 403 FORBIDDEN
 * 참여 취소 요청자가 참여자 본인이 아니다
 */
public class NotParticipationOwnerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotParticipationOwnerException() {
        super("본인의 참여 내역만 취소할 수 있습니다.");
    }
}