package com.mycom.myapp.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.service.ChallengeService;
import com.mycom.myapp.common.ResultDto;
import com.mycom.myapp.common.exception.ChallengeNotFoundException;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class ChallengeServiceIntegrationTest {

	@Autowired
	private ChallengeService challengeService;
	
	@Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private EntityManager em;
	
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
		// given
		Long id = 41L;
		ChallengeDto request = ChallengeDto.builder().id(id)
											.title("테스트 챌린지")
											.description("테스트 챌린지다.")
											.depositAmount(10000)
							                .requiredCount(5)
							                .startDate(LocalDate.now())
							                .endDate(LocalDate.now().plusDays(7))
											.build();
		
		// when
		ResultDto<Long> result = challengeService.updateChallenge(request);
		
		em.flush();
        em.clear();
		

        // then
		Optional<Challenge> updatedChallenge = challengeRepository.findById(id);
		
		assertNotNull(updatedChallenge);
		assertEquals("테스트 챌린지", updatedChallenge.get().getTitle());
		
		assertEquals("success", result.getResult());
		
	}
	
	@Test
	@Transactional
    void 챌린지_삭제_테스트() {
        // ==========================================
        // [1] GIVEN: 삭제 타겟 엔티티 사전 영속화
        // ==========================================
//        Long hostId = 100L;
        Challenge challenge = Challenge.builder()
                .title("삭제 대상 챌린지")
                .description("테스트용 데이터")
                .depositAmount(10000)
                .requiredCount(5)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .createdAt(LocalDateTime.now())
//                .hostId(hostId)
                .build();

        challengeRepository.save(challenge);
        Long targetId = challenge.getId();

        // ==========================================
        // [2] WHEN: 삭제 서비스 파이프라인 격발
        // ==========================================
        ResultDto<Void> result = challengeService.deleteChallenge(targetId);

        // 🎯 핵심 : 쓰기 지연 DELETE 쿼리를 DB에 즉시 사출하고 1차 캐시 완전 휘발
        em.flush();
        em.clear();

        // ==========================================
        // [3] THEN: 정밀 검증
        // ==========================================
        // 검증 1: 서비스 응답 아키텍처 규격 검증
        assertThat(result).isNotNull();
        assertThat(result.getResult()).isEqualTo("success");
//        assertThat(result.getData()).isEqualTo(targetId); // Void 타입

        // 검증 2: 실물 DB 재조회 시 데이터 존재하지 않음(Hard Delete) 검증
        Optional<Challenge> deletedChallenge = challengeRepository.findById(targetId);
        assertThat(deletedChallenge).isEmpty();

        // 검증 3: 삭제된 식별자로 단건 조회 요청 시 NOT_FOUND 예외 사출 검증
        assertThatThrownBy(() -> challengeService.detailChallenge(targetId))
                .isInstanceOf(ChallengeNotFoundException.class)
                .hasMessage("챌린지를 찾을 수 없습니다. id=" + targetId);
//                .extracting("errorCode")
//                .isEqualTo(ErrorCode.CHALLENGE_NOT_FOUND);
    }
}
