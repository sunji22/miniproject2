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
import com.mycom.myapp.user.entity.Role;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class ChallengeServiceTest {

	@Autowired
	private ChallengeService challengeService;
	
	@Autowired
    private ChallengeRepository challengeRepository;
	
	@Autowired
	private UserRepository userRepository;

    @Autowired
    private EntityManager em;
	
    // 등록까지가 MVP
	@Test
	@Transactional
	void 등록_테스트() {
		
		// given
	    User user = new User();
	    user.setEmail("test@naver.com"); user.setName("잇으지");
	    user.setPassword("1234"); user.setRole(Role.USER);
	    // 개설자는 개설 즉시 참여 -> 보증금이 잠기므로 잔액이 있어야 한다
	    user.setPointBalance(100000);

	    User savedUser = userRepository.save(user); // 임시 저장 -> 끝나면 롤백
	    
	    log.info("userId={}", savedUser.getUserId());
	    
		// 등록/수정용 파라미터
		ChallengeDto challengeDto = ChallengeDto.builder()
												.hostId(savedUser.getUserId())
												.title("테스트 챌린지")
												.description("테스트 챌린지다.")
												.depositAmount(10000)
								                .requiredCount(5)
								                .startDate(LocalDate.now())
								                .endDate(LocalDate.now().plusDays(7))
												.build();
		
		// when - 서비스는 raw data(id)를 반환하고, 요청자 userId 를 인자로 받는다
		Long savedId = challengeService.insertChallenge(challengeDto, savedUser.getUserId());

		log.info("챌린지 id={}", savedId);

		// then
		assertNotNull(savedId);
		assertEquals(savedUser.getUserId(),
				challengeRepository.findById(savedId).get().getHost().getUserId());
	}

	// 테스트가 스스로 쓸 host + 챌린지를 만든다 (하드코딩 id 에 의존하지 않도록)
	private User saveHost(String email) {
		User user = new User();
		user.setEmail(email);
		user.setName("호스트");
		user.setPassword("1234");
		user.setRole(Role.USER);
		user.setPointBalance(100000);
		return userRepository.save(user);
	}

	private Challenge saveChallenge(User host) {
		Challenge challenge = Challenge.builder()
				.host(host)
				.title("원본 챌린지")
				.description("테스트용 데이터")
				.depositAmount(10000)
				.requiredCount(5)
				.startDate(LocalDate.now())
				.endDate(LocalDate.now().plusDays(7))
				.status(ChallengeStatus.RECRUITING)   // 수정/삭제는 RECRUITING 상태만 허용
				.createdAt(LocalDateTime.now())
				.build();
		return challengeRepository.save(challenge);
	}
	
	@Test
	@Transactional
	void 업데이트_테스트() {
		// given - 이 테스트가 직접 만든 host 와 챌린지를 대상으로 한다
		User host = saveHost("update-host@test.com");
		Challenge origin = saveChallenge(host);
		Long id = origin.getId();

		ChallengeDto request = ChallengeDto.builder().id(id)
											.title("테스트 챌린지")
											.description("테스트 챌린지다.")
											.depositAmount(10000)
							                .requiredCount(5)
							                .startDate(LocalDate.now())
							                .endDate(LocalDate.now().plusDays(7))
											.build();

		// when - 요청자 userId 를 같이 넘긴다 (작성자 검증)
		Long updatedId = challengeService.updateChallenge(request, host.getUserId());

		em.flush();
        em.clear();


        // then
		Optional<Challenge> updatedChallenge = challengeRepository.findById(id);

		assertNotNull(updatedChallenge);
		assertEquals("테스트 챌린지", updatedChallenge.get().getTitle());
		assertEquals(id, updatedId);
	}

	@Test
	@Transactional
	void 업데이트_작성자아니면_실패() {
		User host = saveHost("owner@test.com");
		User other = saveHost("other@test.com");
		Challenge origin = saveChallenge(host);

		ChallengeDto request = ChallengeDto.builder().id(origin.getId())
											.title("남이 바꾸려 함")
											.depositAmount(10000)
							                .requiredCount(5)
							                .startDate(LocalDate.now())
							                .endDate(LocalDate.now().plusDays(7))
											.build();

		assertThatThrownBy(() -> challengeService.updateChallenge(request, other.getUserId()))
				.isInstanceOf(RuntimeException.class);
	}
	
	@Test
	@Transactional
    void 챌린지_삭제_테스트() {
        // ==========================================
        // [1] GIVEN: 삭제 타겟 엔티티 사전 영속화
        // ==========================================
        // 삭제는 요청자=작성자 검증을 하므로 host 를 반드시 채워야 한다
        User host = saveHost("delete-host@test.com");
        Challenge challenge = saveChallenge(host);
        challenge.setTitle("삭제 대상 챌린지");

        Long targetId = challenge.getId();

        // ==========================================
        // [2] WHEN: 삭제 서비스 파이프라인 격발
        // ==========================================
        challengeService.deleteChallenge(targetId, host.getUserId());

        // 🎯 핵심 : 쓰기 지연 DELETE 쿼리를 DB에 즉시 사출하고 1차 캐시 완전 휘발
        em.flush();
        em.clear();

        // ==========================================
        // [3] THEN: 정밀 검증
        // ==========================================
        // 검증 1: 실물 DB 재조회 시 데이터 존재하지 않음(Hard Delete) 검증
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
