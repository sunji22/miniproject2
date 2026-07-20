package com.mycom.myapp.challenge.service;

import java.util.List;

import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeResultDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.common.ResultDto;

public interface ChallengeService {

	// 목록
	ResultDto<List<ChallengeDto>> listChallenge(ChallengeSearchConditionDto conditionDto);
}
