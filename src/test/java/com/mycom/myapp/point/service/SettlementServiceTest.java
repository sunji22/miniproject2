package com.mycom.myapp.point.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.entity.ParticipationStatus;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.common.exception.SettlementAlreadyDoneException;
import com.mycom.myapp.point.repository.PointHistoryRepository;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

@SpringBootTest
@Transactional
public class SettlementServiceTest {

	@Autowired
	private SettlementService settlementService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ChallengeRepository challengeRepository;
	
	@Autowired
	private ParticipationRepository participationRepository;
	
	@Autowired
	private PointHistoryRepository pointHistoryRepository;
	
	// 테스트 공통 데이터
	private User user;
	private Challenge challenge;
	private Participation participation;
	
	// 각 테스트 전에 공통 데이터 세팅
	@BeforeEach
	void setUp() {
		// 유저 생성 (잔액 10000)
		user = userRepository.save(
				new User(null, "test@test.com", "1234", "홍길동", "ROLE_USER", 10000, null)
		);
		
		// 챌린지 생성 (보증금 3000)
		challenge = challengeRepository.save(
				new Challenge(null, user, "챌린지1", "설명", 3000, 3, LocalDate.now()
								, LocalDate.now().plusDays(7), "RECRUITING", null)
		);
		
		// 참여 생성 (상태 : JOINED)
		participation = participationRepository.save(
				new Participation(null, challenge, user, 0, ParticipationStatus.JOINED, null)
		);
	}
	
	//====================== penaltyAll 테스트 =============================
	@Test
	@DisplayName("전원 실패 시 보증금 전액 몰수")
	void penaltyAll_failed() {
		// 유저 잔액 10000, 보증금 3000
		settlementService.penaltyAll(challenge.getId());
		
		// 유저 잔액 10000 - 3000 = 7000
		User updated = userRepository.findById(user.getId()).get();
		assertEquals(7000, updated.getPointBalance());
		
		// PointHistory PENALTY 이력 저장 확인
		List<PointHistory> history = pointHistoryRepository
				.findByUser_UserIdOrderByCreatedAtDesc(user.getId());
		assertEquals(1, history.size());
		assertEquals(PointType.PENALTY, history.get(0).getType());
		assertEquals(3000, history.get(0).getStatus());
		
		// 챌린지 상태 CLOSED 변경 확인
		Challenge updatedChallenge = challengeRepository.findById(challenge.getId()).get();
		assertEquals("CLOSED", updatedChallenge.getStatus());
	}
	
	@Test
	@DisplayName("이미 정산된 챌린지는 penaltyAll 시 예외")
	void penaltyAll_settled() {
		// 챌린지 CLOSED 상태로 변경
		challenge.setStatus("CLOSED");
		challengeRepository.save(challenge);
		
		// SettlementAlreadyDoneException 발생
		assertThrows(SettlementAlreadyDoneException.class,
				() -> settlementService.penaltyAll(challenge.getId()));
	}
	
	// ======================== refund 테스트 ==============================
	
	@Test
	@DisplayName("환불 처리 - 유저 잔액 증가 + DEPOSIT_REFUND 이력 저장")
	void refund_success() {
		// 보증금 3000 환불
		int refundAmount = 3000;
		
		// refund 호출
		settlementService.refund(user.getId(), participation.getId(), refundAmount);
		
		// 유저 잔액 10000 + 3000 = 13000
		User updated = userRepository.findById(user.getId()).get();
		assertEquals(13000, updated.getPointBalance);
		
		// PointHistory DEPOSIT_REFUND 이력 저장 확인
		List<PointHistory> history = pointHistoryRepository
				.findByUser_UserIdOrderByCreatedAtDesc(user.getId());
		assertEquals(1, history.size());
		assertEquals(PointType.REFUND, history.get(0).getType());
		assertEquals(3000, history.get(0).getStatus());
		
		// 챌린지 상태 CLOSED 변경 확인
		Challenge updatedChallenge = challengeRepository.findById(challenge.getId()).get();
		assertEquals("CLOSED", updatedChallenge.getStatus());
	}
	
	// ======================== penalty 테스트 ==============================
	
	@Test
	@DisplayName("개별 몰수 처리 - 유저 잔액 감소 + PENALTY 이력 저장")
	void penalty_success() {
		// 보증금 3000 환불
		int penaltyAmount = 3000;
		
		// penalty 호출
		settlementService.penalty(user.getId(), participation.getId(), penaltyAmount);
		
		// 유저 잔액 10000 - 3000 = 7000
		User updated = userRepository.findById(user.getId()).get();
		assertEquals(7000, updated.getPointBalance);
		
		// PointHistory DEPOSIT_REFUND 이력 저장 확인
		List<PointHistory> history = pointHistoryRepository
				.findByUser_UserIdOrderByCreatedAtDesc(user.getId());
		assertEquals(1, history.size());
		assertEquals(PointType.PENALTY, history.get(0).getType());
		
		// 챌린지 상태 CLOSED 변경 확인
		Challenge updatedChallenge = challengeRepository.findById(challenge.getId()).get();
		assertEquals("CLOSED", updatedChallenge.getStatus());
	}
	
	// ======================== reward 테스트 ==============================
	
	@Test
	@DisplayName("분배 처리 - 유저 잔액 증가 + REWARD 이력 저장")
	void reward_success() {
		// 총 몰수액 9000 (참여자 3명), 성공자 2명
		int totalPenaltyAmount = 9000;
		int successCount = 2;
		
		// reward 호출
		settlementService.reward(user.getId(), participation.getId(), totalPenaltyAmount, successCount);
		
		// 유저 잔액 10000 + 4500 = 14500
		User updated = userRepository.findById(user.getId()).get();
		assertEquals(14500, updated.getPointBalance);
		
		// PointHistory DEPOSIT_REFUND 이력 저장 확인
		List<PointHistory> history = pointHistoryRepository
				.findByUser_UserIdOrderByCreatedAtDesc(user.getId());
		assertEquals(1, history.size());
		assertEquals(PointType.REWARD, history.get(0).getType());
		assertEquals(4500, history.get(0).getAmount());
		
		// 챌린지 상태 CLOSED 변경 확인
		Challenge updatedChallenge = challengeRepository.findById(challenge.getId()).get();
		assertEquals("CLOSED", updatedChallenge.getStatus());
	}
	
	
}
