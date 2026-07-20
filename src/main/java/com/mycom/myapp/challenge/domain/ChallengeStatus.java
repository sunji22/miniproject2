package com.mycom.myapp.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum ChallengeStatus {
    RECRUITING("모집중"),
    ONGOING("진행중"),
    CLOSED("종료됨");

    private final String description;

    ChallengeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }
    
    // 🎯 Jackson 이 JSON 문자열을 Enum 으로 변환할 때 이 메서드를 강제 실행.
    @JsonCreator 
    public static ChallengeStatus from(String value) {
        return Arrays.stream(ChallengeStatus.values())
                .filter(status -> status.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("잘못된 모집 상태입니다. 수용 가능 스펙: %s", Arrays.toString(ChallengeStatus.values()))
                ));
    }
}