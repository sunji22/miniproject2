package com.mycom.myapp.common.exception;

/**
 * 409 CONFLICT
 * 진행 중인 챌린지 삭제를 시도할 때
 */
public class CannotDeleteOngoingChallengeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CannotDeleteOngoingChallengeException() {
        super("진행 중인 챌린지는 삭제할 수 없습니다.");
    }
}