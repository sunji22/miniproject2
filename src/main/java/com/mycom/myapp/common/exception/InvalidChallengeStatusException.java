package com.mycom.myapp.common.exception;

/**
 * 400 BAD_REQUEST
 * 모집중이 아닌(진행중/종료됨) 챌린지 내용 수정을 시도할 때
 */
public class InvalidChallengeStatusException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidChallengeStatusException() {
        super("모집 중인 챌린지만 수정할 수 있습니다.");
    }
}