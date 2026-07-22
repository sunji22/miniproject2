package com.mycom.myapp.challenge.service;

import java.util.List;

import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.common.ResultDto;

public interface ChallengeService {

	// 목록
	ResultDto<List<ChallengeDto>> listChallenge(ChallengeSearchConditionDto searchCondition);
	
	// 상세
	ResultDto<ChallengeDto> detailChallenge(Long id);
	
	// 등록
	ResultDto<Long> insertChallenge(ChallengeDto challengeDto);
	
	// 수정
	ResultDto<Long> updateChallenge(ChallengeDto challengeDto);
	
	// 삭제
	ResultDto<Void> deleteChallenge(Long id);
	
	// 타 도메인에서 사용할 '유효성 검증이 완료된 Challenge 엔티티 반환 메소드'
	public Challenge getValidChallenge(Long id);
}
