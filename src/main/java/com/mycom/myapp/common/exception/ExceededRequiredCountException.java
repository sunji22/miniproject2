package com.mycom.myapp.common.exception;

/**
 * 400 BAD_REQUEST
 * (사용자 입력) 인증 최소 횟수가 챌린지 총 기간보다 클 때
 */
public class ExceededRequiredCountException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExceededRequiredCountException(long totalDays) {
        super("인증 필요 횟수가 총 챌린지 기간(" + totalDays + "일)을 초과할 수 없습니다.");
    }
}