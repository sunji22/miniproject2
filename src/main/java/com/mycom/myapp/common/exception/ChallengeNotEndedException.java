package com.mycom.myapp.common.exception;

// 아직 종료되지 않은 챌린지에 정산을 요청할 때 던지는 예외
public class ChallengeNotEndedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChallengeNotEndedException(Long challengeId) {
        super("아직 종료되지 않은 챌린지입니다. id=" + challengeId);
    }
}
