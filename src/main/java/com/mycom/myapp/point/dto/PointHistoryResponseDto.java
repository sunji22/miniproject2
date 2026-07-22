package com.mycom.myapp.point.dto;

import java.time.LocalDateTime;

import com.mycom.myapp.point.entity.PointHistory;
import com.mycom.myapp.point.entity.PointType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PointHistoryResponseDto {
	private int balance;
	private Long pointHistoryId;
	private int amount;
	private PointType type;
	private int balanceAfter;
	private LocalDateTime createdAt;
	
	public static PointHistoryResponseDto from(PointHistory entity) {
		return PointHistoryResponseDto.builder()
				.pointHistoryId(entity.getPointHistoryId())
				.amount(entity.getAmount())
				.type(entity.getType())
				.balanceAfter(entity.getBalanceAfter())
				.createdAt(entity.getCreatedAt())
				.build();
	}
}
