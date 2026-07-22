package com.mycom.myapp.point.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.domain.SettlementStatus;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.entity.ParticipationStatus;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.common.exception.SettlementAlreadyDoneException;
import com.mycom.myapp.point.entity.PointHistory;
import com.mycom.myapp.point.entity.PointType;
import com.mycom.myapp.point.repository.PointHistoryRepository;
import com.mycom.myapp.user.entity.Role;
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
		// 유저 생성 (잔액 10000) - User에 @AllArgsConstructor 없으므로 setter 사용
		User testUser = new User();
		testUser.setEmail("test@test.com");
		testUser.setPassword("1234");
		testUser.setName("홍길동");
		testUser.setRole(Role.USER);
		testUser.setPointBalance(10000);
		testUser.setCreatedAt(LocalDateTime.now());
		user = userRepository.save(testUser);

		// 챌린지 생성 (보증금 3000) - enum 타입 사용 + settlementStatus 추가
		challenge = challengeRepository.save(
				Challenge.builder()
						.host(user)
						.title("챌린지1")
						.description("설명")
						.depositAmount(3000)
						.requiredCount(3)
						.startDate(LocalDate.now())
						.endDate(LocalDate.now().plusDays(7))
						.status(ChallengeStatus.RECRUITING)
						.settlementStatus(SettlementStatus.PENDING)
						.createdAt(LocalDateTime.now())
						.build()
		);

		// 참여 생성 (상태 : JOINED)
		participation = participationRepository.save(
		        new Participation(null, challenge, user, 0, ParticipationStatus.JOINED, LocalDateTime.now())
		);
	}

	//====================== penaltyAll 테스트 =============================
	@Test
	@DisplayName("전원 실패 시 보증금 전액 몰수")
	void penaltyAll_failed() {
		// 유저 잔액 10000, 보증금 3000
		settlementService.penaltyAll(challenge.getId());

		// 유저 잔액 10000 - 3000 = 7000
		User updated = userRepository.findById(user.getUserId()).get();
		assertEquals(7000, updated.getPointBalance());

		// PointHistory PENALTY 이력 저장 확인
		List<PointHistory> history = pointHistoryRepository
				.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
		assertEquals(1, history.size());
		assertEquals(PointType.PENALTY, history.get(0).getType());
		// PointHistory에는 getStatus() 없음, amount 확인
		assertEquals(3000, history.get(0).getAmount());
	}

	@Test
	@DisplayName("이미 정산된 챌린지는 penaltyAll 시 예외")
	void penaltyAll_settled() {
		// 챌린지 settlementStatus를 SETTLED로 변경 (정산 완료 상태로 변경)
		challenge.setSettlementStatus(SettlementStatus.SETTLED);
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
		settlementService.refund(user.getUserId(), participation.getId(), refundAmount);

		// 유저 잔액 10000 + 3000 = 13000
		User updated = userRepository.findById(user.getUserId()).get();
		assertEquals(13000, updated.getPointBalance());

		// PointHistory DEPOSIT_REFUND 이력 저장 확인
		List<PointHistory> history = pointHistoryRepository
				.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
		assertEquals(1, history.size());
		assertEquals(PointType.DEPOSIT_REFUND, history.get(0).getType());
		// PointHistory에는 getStatus() 없음, amount 확인
		assertEquals(3000, history.get(0).getAmount());
	}

	// ======================== penalty 테스트 ==============================

	@Test
	@DisplayName("개별 몰수 처리 - 유저 잔액 감소 + PENALTY 이력 저장")
	void penalty_success() {
		// 몰수 금액 3000
		int penaltyAmount = 3000;

		// penalty 호출
		settlementService.penalty(user.getUserId(), participation.getId(), penaltyAmount);

		// 유저 잔액 10000 - 3000 = 7000
		User updated = userRepository.findById(user.getUserId()).get();
		assertEquals(7000, updated.getPointBalance());

		// PointHistory PENALTY 이력 저장 확인
		List<PointHistory> history = pointHistoryRepository
				.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
		assertEquals(1, history.size());
		assertEquals(PointType.PENALTY, history.get(0).getType());
		// PointHistory에는 getStatus() 없음, amount 확인
		assertEquals(3000, history.get(0).getAmount());
	}

	// ======================== reward 테스트 ==============================

	@Test
	@DisplayName("분배 처리 - 유저 잔액 증가 + REWARD 이력 저장")
	void reward_success() {
		// 총 몰수액 9000 (참여자 3명), 성공자 2명
		int totalPenaltyAmount = 9000;
		int successCount = 2;

		// reward 호출
		settlementService.reward(user.getUserId(), participation.getId(), totalPenaltyAmount, successCount);

		// 유저 잔액 10000 + 4500 = 14500 (9000 / 2 = 4500)
		User updated = userRepository.findById(user.getUserId()).get();
		assertEquals(14500, updated.getPointBalance());

		// PointHistory REWARD 이력 저장 확인
		List<PointHistory> history = pointHistoryRepository
				.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
		assertEquals(1, history.size());
		assertEquals(PointType.REWARD, history.get(0).getType());
		assertEquals(4500, history.get(0).getAmount());
	}
}
