package com.mycom.myapp.challenge;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.service.ChallengeServiceImpl;
import com.mycom.myapp.common.ResultDto;
import com.mycom.myapp.common.exception.ChallengeNotFoundException;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class ChallengeFindTest {

	@Autowired
	private ChallengeRepository challengeRepository;
	
	@Autowired
	private ChallengeServiceImpl challengeServiceImpl;
	
	@Test
	void 목록조회_테스트() {
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
	
	@Test
	void 상세조회_테스트() {
		log.info("상세 조회 테스트");
		
		Challenge entity = challengeRepository.findById(41L).get();
		ResultDto<ChallengeDto> result = challengeServiceImpl.detailChallenge(41L);
		
		log.info("id={}, 제목={}", entity.getId(), entity.getTitle());
		
		assertNotNull(entity);
		assertEquals(41L, entity.getId());
		assertNotNull(result.getData());
		assertEquals(41L, result.getData().getId());
		assertEquals("success", result.getResult());
		
	}
	
	@Test
	void NotFound_예외_테스트() {
	    // given
	    Long invalidChallengeId = -1L;

	    // when & then
	    ChallengeNotFoundException exception = assertThrows(ChallengeNotFoundException.class, () -> {
	        challengeServiceImpl.detailChallenge(invalidChallengeId);
	    });

	    log.info(exception.getMessage());
	    // 추가 검증: 에러 메시지 정합성 확인
	    assertEquals("챌린지를 찾을 수 없습니다. id="+invalidChallengeId, exception.getMessage());
	}
}
