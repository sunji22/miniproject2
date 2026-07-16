package com.mycom.myapp.challenge.service;

import com.mycom.myapp.challenge.dto.ChallengeResultDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;

public interface ChallengeService {

	// 목록
	ChallengeResultDto listChallenge(ChallengeSearchConditionDto conditionDto);
}
