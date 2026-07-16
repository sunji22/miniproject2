package com.mycom.myapp.common.exception;

// 챌린지 참여 시 지갑 잔액(point_balance)이 보증금보다 적어 참여할 수 없을 때 던지는 예외
public class InsufficientPointException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InsufficientPointException(long required, long balance) {
        super("포인트가 부족합니다. 필요=" + required + ", 보유=" + balance);
    }
}
