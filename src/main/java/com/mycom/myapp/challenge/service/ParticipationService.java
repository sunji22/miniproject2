package com.mycom.myapp.challenge.service;

public interface ParticipationService {

	// 참여 테이블 등록
	public Long participate(Long challengeId, Long userId);
}
