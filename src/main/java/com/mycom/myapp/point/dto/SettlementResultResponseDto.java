package com.mycom.myapp.point.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettlementResultResponseDto {
	private Long userId;
	private Long challengeId;
	private int amout;
	private int balanceAfter;
}
