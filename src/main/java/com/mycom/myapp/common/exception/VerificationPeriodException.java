package com.mycom.myapp.common.exception;

import java.time.LocalDate;

// 챌린지 진행 기간 밖에서 인증글을 등록하려 할 때 던지는 예외
public class VerificationPeriodException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VerificationPeriodException(LocalDate startDate, LocalDate endDate) {
        super("챌린지 진행 기간이 아닙니다. 기간=" + startDate + " ~ " + endDate);
    }
}
