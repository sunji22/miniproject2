package com.mycom.myapp.challenge.domain;

public enum SettlementStatus {
	PENDING("정산 대기"),
	SETTLED("정산 완료");
	
	private final String description;
	
	SettlementStatus(String description){
		this.description = description;
	}
	
	public String getDescription() {
		return this.description;
	}
}
