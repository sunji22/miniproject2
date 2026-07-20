package com.mycom.myapp.common.exception;

// 이미 정산(CLOSED)된 챌린지에 정산을 다시 요청할 때 던지는 예외
public class SettlementAlreadyDoneException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SettlementAlreadyDoneException(Long challengeId) {
        super("이미 정산된 챌린지입니다. id=" + challengeId);
    }
}
