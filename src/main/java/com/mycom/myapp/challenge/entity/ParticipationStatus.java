package com.mycom.myapp.challenge.entity;

public enum ParticipationStatus {
	JOINED("참여중"),
	SUCCESS("성공"),
	FAILED("실패"),
	CANCLED("취소됨");
	
	private final String description;

	ParticipationStatus(String description) {
		this.description = description;
	}
	
    public String getDescription() {
        return this.description;
    }
}
