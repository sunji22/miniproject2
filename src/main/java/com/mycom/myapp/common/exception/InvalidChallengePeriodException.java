package com.mycom.myapp.common.exception;

/**
 * 400 BAD_REQUEST
 * (사용자 입력) 챌린지 시작일이 종료일 이후일 때
 */
public class InvalidChallengePeriodException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidChallengePeriodException() {
        super("시작일은 종료일보다 이전이어야 합니다");
    }
}