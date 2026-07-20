package com.mycom.myapp.challenge.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DB -> 엔티티 -> 응답용 조회 결과 DTO
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDto {

	private Long id;			// 등록 시에는 사용 x (Auto Increment)
	
//	private Long hostId;		// User 엔티티 추가 후
	
	private String title;
	private String description; // 상세 조회 시에만 사용
	private int depositAmount;
	private int requiredCount; 
	private LocalDate startDate; 
	private LocalDate endDate; 
	private String status;		// 등록 시에는 사용 x (Default: 모집중)
	private LocalDateTime createdAt; //
}
