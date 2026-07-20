package com.mycom.myapp.challenge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.service.ChallengeService;
import com.mycom.myapp.common.ResultDto;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class ChallengeServiceIntegrationTest {

	@Autowired
	private ChallengeService challengeService;
	
	
	@Test
	@Transactional
	void 등록_테스트() {
		// 등록/수정용 파라미터
		ChallengeDto challengeDto = ChallengeDto.builder()
												.title("테스트 챌린지")
												.description("테스트 챌린지다.")
												.depositAmount(10000)
								                .requiredCount(5)
								                .startDate(LocalDate.now())
								                .endDate(LocalDate.now().plusDays(7))
												.build();
		
		Challenge challenge = challengeDto.toEntity();
		log.info("status={}", challengeDto.getStatus());// dto 는 null
		log.info("status={}", challenge.getStatus());	// default 값 RECRUITING 확인
		
		ResultDto<Long> result = challengeService.insertChallenge(challengeDto);
		
		log.info("챌린지 id={}", result.getData());
		
		// then
		assertEquals("success", result.getResult());
		assertEquals(ChallengeStatus.RECRUITING, challenge.getStatus());
		
	}
	
	@Test
	@Transactional
	void 업데이트_테스트() {
		
	}
}
