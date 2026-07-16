package com.mycom.myapp.challenge.dto;

import java.util.List;

import lombok.Data;

@Data
public class ChallengeResultDto {

	private String result;
	private ChallengeDto challengeDto;	// 상세 조회용
	private List<ChallengeDto> list;	// 목록 조회용
	private int count;					// 총 건수
}
