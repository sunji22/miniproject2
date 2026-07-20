package com.mycom.myapp.challenge;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.service.ChallengeServiceImpl;
import com.mycom.myapp.common.ResultDto;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class ChallengeFindTest {

	@Autowired
	private ChallengeRepository challengeRepository;
	
	@Autowired
	private ChallengeServiceImpl challengeServiceImpl;
	
	@Test
	void testFindList() {
		log.info("목록 조회 테스트");
		ChallengeSearchConditionDto condition = new ChallengeSearchConditionDto();
		condition.setTitle("일찍 자기");
		condition.setStatus(ChallengeStatus.RECRUITING);
		
		ResultDto<List<ChallengeDto>> result = challengeServiceImpl.listChallenge(condition);
		
		log.info(result.getData().toString());
		
		assertNotNull(challengeServiceImpl);
		assertNotNull(result);
		assertEquals("success", result.getResult());
	}
}
