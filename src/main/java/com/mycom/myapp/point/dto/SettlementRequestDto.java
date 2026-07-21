package com.mycom.myapp.point.dto;

import lombok.Data;

@Data
public class SettlementRequestDto {
	private Long challengeId;
	private Long participationId;
	private int amount;
	private int totalPenaltyAmount;
	private int successCount;
}
