package com.mycom.myapp.challenge.service;

import java.util.List;

import com.mycom.myapp.challenge.dto.MyParticipationResponseDto;
import com.mycom.myapp.challenge.dto.ParticipantResponseDto;

public interface ParticipationService {

	// 참여 테이블 등록
	public Long participate(Long challengeId, Long userId);
	
	// 참여자 목록 조회
	public List<ParticipantResponseDto> listParticipant(Long challengeId);
	
	// 내 참여 목록 조회
	public List<MyParticipationResponseDto> listMyParticipation(Long userId);
}
