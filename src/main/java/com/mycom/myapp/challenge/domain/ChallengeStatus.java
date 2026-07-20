package com.mycom.myapp.challenge.domain;

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
}