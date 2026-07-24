package com.mycom.myapp.common.exception;

// 조회하려는 참여가 DB 에 없을 때 서비스 계층에서 던지는 사용자 정의 예외
// 404 NOT_FOUND
public class ParticipationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ParticipationNotFoundException(Long challengeId) {
        super("해당 챌린지 참여 기록이 없습니다. id=" + challengeId);
    }
}