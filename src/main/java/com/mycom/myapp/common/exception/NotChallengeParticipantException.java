package com.mycom.myapp.common.exception;

// 해당 챌린지에 참여하지 않은 회원이 인증글을 쓰거나 상호체크를 하려 할 때 던지는 예외
public class NotChallengeParticipantException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotChallengeParticipantException(Long challengeId) {
        super("참여하지 않은 챌린지입니다. challengeId=" + challengeId);
    }
}
