package com.mycom.myapp.point.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PointHistoryResponseDto {
	private int balance;
}
