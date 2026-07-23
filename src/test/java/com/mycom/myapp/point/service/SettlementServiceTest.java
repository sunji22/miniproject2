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
import org.springframework.security.access.AccessDeniedException;
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

	// ===== 테스트 공통 데이터 (다중 사용자) =====
	private User host;					// 호스트 (정산 실행 주체)
	private User participant1;			// 참여자1 (성공)
	private User participant2;			// 참여자2 (성공)
	private User participant3;			// 참여자3 (실패)
	private Challenge challenge;
	private Participation p1;			// 참여자1의 참여 정보
	private Participation p2;			// 참여자2의 참여 정보
	private Participation p3;			// 참여자3의 참여 정보

	// ===== 각 테스트 전에 공통 데이터 세팅 =====
	@BeforeEach
	void setUp() {
		// 호스트 생성 (잔액 10000)
		host = userRepository.save(createUser("host@test.com", "홍길동", 10000));

		// 참여자 생성 (각 잔액 10000)
		participant1 = userRepository.save(createUser("user1@test.com", "이순신", 10000));
		participant2 = userRepository.save(createUser("user2@test.com", "강감찬", 10000));
		participant3 = userRepository.save(createUser("user3@test.com", "을지문덕", 10000));

		// 챌린지 생성 (보증금 3000, 성공조건 3회)
		challenge = challengeRepository.save(
				Challenge.builder()
						.host(host)
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

		// 참여자1: 3회 성공 (성공 조건 충족)
		p1 = participationRepository.save(
				new Participation(null, challenge, participant1, 3, ParticipationStatus.SUCCESS, LocalDateTime.now()));

		// 참여자2: 3회 성공 (성공 조건 충족)
		p2 = participationRepository.save(
				new Participation(null, challenge, participant2, 3, ParticipationStatus.SUCCESS, LocalDateTime.now()));

		// 참여자3: 1회만 (성공 조건 미충족 → 실패)
		p3 = participationRepository.save(
				new Participation(null, challenge, participant3, 1, ParticipationStatus.FAILED, LocalDateTime.now()));
	}

	// User 생성 헬퍼 메서드 (중복 제거)
	private User createUser(String email, String name, int pointBalance) {
		User user = new User();
		user.setEmail(email);
		user.setPassword("1234");
		user.setName(name);
		user.setRole(Role.USER);
		user.setPointBalance(pointBalance);
		user.setCreatedAt(LocalDateTime.now());
		return user;
	}

	//====================== penaltyAll 테스트 =============================

	@Test
	@DisplayName("전원 실패 시 보증금 전액 몰수")
	void penaltyAll_failed() {
	    // 전원 실패 시뮬레이션을 위해 참여자들의 상태를 FAILED로 맞추거나, 
	    // 혹은 penaltyAll이 참여자 전체를 몰수하는 메서드라면 실행
	    settlementService.penaltyAll(challenge.getId());

	    // 수정: 호스트(host)가 아니라 참여자1(participant1)의 잔액이 몰수되어 7000원이 되었는지 검증
	    User updated = userRepository.findById(participant1.getUserId()).get();
	    assertEquals(7000, updated.getPointBalance());

	    // PointHistory PENALTY 이력 저장 확인도 참여자1 기준으로 변경
	    List<PointHistory> history = pointHistoryRepository
	            .findByUser_UserIdOrderByCreatedAtDesc(participant1.getUserId());
	    assertEquals(1, history.size());
	    assertEquals(PointType.PENALTY, history.get(0).getType());
	    assertEquals(3000, history.get(0).getAmount());
	}

	@Test
	@DisplayName("이미 정산된 챌린지는 penaltyAll 시 예외")
	void penaltyAll_settled() {
		// 챌린지 settlementStatus를 SETTLED로 변경
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
		int refundAmount = 3000;

		// 참여자1에게 보증금 환불
		settlementService.refund(participant1.getUserId(), p1.getId(), refundAmount);

		// 유저 잔액 10000 + 3000 = 13000
		User updated = userRepository.findById(participant1.getUserId()).get();
		assertEquals(13000, updated.getPointBalance());

		// PointHistory DEPOSIT_REFUND 이력 저장 확인
		List<PointHistory> history = pointHistoryRepository
				.findByUser_UserIdOrderByCreatedAtDesc(participant1.getUserId());
		assertEquals(1, history.size());
		assertEquals(PointType.DEPOSIT_REFUND, history.get(0).getType());
		assertEquals(3000, history.get(0).getAmount());
	}

	// ======================== penalty 테스트 ==============================

	@Test
	@DisplayName("개별 몰수 처리 - 유저 잔액 감소 + PENALTY 이력 저장")
	void penalty_success() {
		int penaltyAmount = 3000;

		// 참여자3에게 몰수
		settlementService.penalty(participant3.getUserId(), p3.getId(), penaltyAmount);

		// 유저 잔액 10000 - 3000 = 7000
		User updated = userRepository.findById(participant3.getUserId()).get();
		assertEquals(7000, updated.getPointBalance());

		// PointHistory PENALTY 이력 저장 확인
		List<PointHistory> history = pointHistoryRepository
				.findByUser_UserIdOrderByCreatedAtDesc(participant3.getUserId());
		assertEquals(1, history.size());
		assertEquals(PointType.PENALTY, history.get(0).getType());
		assertEquals(3000, history.get(0).getAmount());
	}

	// ======================== reward 테스트 ==============================

	@Test
	@DisplayName("분배 처리 - 유저 잔액 증가 + REWARD 이력 저장")
	void reward_success() {
		// 총 몰수액 3000 (실패자 1명), 성공자 2명
		int totalPenaltyAmount = 3000;
		int successCount = 2;

		// 참여자1에게 보너스 분배
		settlementService.reward(participant1.getUserId(), p1.getId(), totalPenaltyAmount, successCount);

		// 유저 잔액 10000 + 1500 = 11500 (3000 / 2 = 1500)
		User updated = userRepository.findById(participant1.getUserId()).get();
		assertEquals(11500, updated.getPointBalance());

		// PointHistory REWARD 이력 저장 확인
		List<PointHistory> history = pointHistoryRepository
				.findByUser_UserIdOrderByCreatedAtDesc(participant1.getUserId());
		assertEquals(1, history.size());
		assertEquals(PointType.REWARD, history.get(0).getType());
		assertEquals(1500, history.get(0).getAmount());
	}

	// =================== settleChallenge 통합 테스트 ===================

	@Test
	@DisplayName("정산 통합 - 성공자 2명 + 실패자 1명")
	void settleChallenge_success_mix() {
		// 정산 실행 (호스트가 실행)
		settlementService.settleChallenge(challenge.getId(), host.getUserId());

		// 참여자1 (성공): 보증금 3000 환불 + 보너스 1500 (3000/2) = 잔액 +4500
		User updated1 = userRepository.findById(participant1.getUserId()).get();
		assertEquals(14500, updated1.getPointBalance());

		// 참여자2 (성공): 보증금 3000 환불 + 보너스 1500 = 잔액 +4500
		User updated2 = userRepository.findById(participant2.getUserId()).get();
		assertEquals(14500, updated2.getPointBalance());

		// 참여자3 (실패): 보증금 3000 몰수 = 잔액 -3000
		User updated3 = userRepository.findById(participant3.getUserId()).get();
		assertEquals(7000, updated3.getPointBalance());

		// 챌린지 상태 변경 확인
		Challenge updatedChallenge = challengeRepository.findById(challenge.getId()).get();
		assertEquals(ChallengeStatus.CLOSED, updatedChallenge.getStatus());
		assertEquals(SettlementStatus.SETTLED, updatedChallenge.getSettlementStatus());
	}

	@Test
	@DisplayName("정산 통합 - 전원 성공")
	void settleChallenge_success_all() {
		// 참여자3도 성공으로 변경
		p3.setSuccessCount(3);
		p3.setStatus(ParticipationStatus.SUCCESS);
		participationRepository.save(p3);

		// 정산 실행
		settlementService.settleChallenge(challenge.getId(), host.getUserId());

		// 전원 보증금 환불 (보너스 없음 - 실패자 0명)
		User updated1 = userRepository.findById(participant1.getUserId()).get();
		assertEquals(13000, updated1.getPointBalance());

		User updated2 = userRepository.findById(participant2.getUserId()).get();
		assertEquals(13000, updated2.getPointBalance());

		User updated3 = userRepository.findById(participant3.getUserId()).get();
		assertEquals(13000, updated3.getPointBalance());

		// 챌린지 상태 변경 확인
		Challenge updatedChallenge = challengeRepository.findById(challenge.getId()).get();
		assertEquals(ChallengeStatus.CLOSED, updatedChallenge.getStatus());
		assertEquals(SettlementStatus.SETTLED, updatedChallenge.getSettlementStatus());
	}

	@Test
	@DisplayName("정산 통합 - 전원 실패")
	void settleChallenge_success_allFail() {
		// 참여자1,2도 실패로 변경
		p1.setSuccessCount(1);
		p1.setStatus(ParticipationStatus.FAILED);
		participationRepository.save(p1);

		p2.setSuccessCount(1);
		p2.setStatus(ParticipationStatus.FAILED);
		participationRepository.save(p2);

		// 정산 실행 → penaltyAll() 호출
		settlementService.settleChallenge(challenge.getId(), host.getUserId());

		// 전원 보증금 몰수
		User updated1 = userRepository.findById(participant1.getUserId()).get();
		assertEquals(7000, updated1.getPointBalance());

		User updated2 = userRepository.findById(participant2.getUserId()).get();
		assertEquals(7000, updated2.getPointBalance());

		User updated3 = userRepository.findById(participant3.getUserId()).get();
		assertEquals(7000, updated3.getPointBalance());

		// 챌린지 상태 변경 확인
		Challenge updatedChallenge = challengeRepository.findById(challenge.getId()).get();
		assertEquals(ChallengeStatus.CLOSED, updatedChallenge.getStatus());
		assertEquals(SettlementStatus.SETTLED, updatedChallenge.getSettlementStatus());
	}

	@Test
	@DisplayName("정산 통합 - 호스트가 아닌 사용자가 정산 시도 시 예외")
	void settleChallenge_notHost() {
		// 참여자1이 정산 시도 → AccessDeniedException 발생
		assertThrows(AccessDeniedException.class,
				() -> settlementService.settleChallenge(challenge.getId(), participant1.getUserId()));
	}

	@Test
	@DisplayName("정산 통합 - 이미 정산된 챌린지 재정산 시 예외")
	void settleChallenge_alreadySettled() {
		// 1차 정산 실행
		settlementService.settleChallenge(challenge.getId(), host.getUserId());

		// 2차 정산 시도 → SettlementAlreadyDoneException 발생
		assertThrows(SettlementAlreadyDoneException.class,
				() -> settlementService.settleChallenge(challenge.getId(), host.getUserId()));
	}
}