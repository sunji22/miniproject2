package com.mycom.myapp.common.exception;

/**
 * 403 FORBIDDEN
 * 주최자가 아닌 사용자가 챌린지 수정/삭제를 시도할 때
 */
public class NotChallengeHostException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotChallengeHostException() {
        super("해당 챌린지를 수정/삭제할 권한이 없습니다.");
    }
}