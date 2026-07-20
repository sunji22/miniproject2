package com.mycom.myapp.common.exception;

// 조회하려는 챌린지가 DB 에 없을 때 서비스 계층에서 던지는 사용자 정의 예외
public class ChallengeNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChallengeNotFoundException(Long challengeId) {
        super("챌린지를 찾을 수 없습니다. id=" + challengeId);
    }
}