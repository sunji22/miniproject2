package com.mycom.myapp.point.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.domain.SettlementStatus;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.common.exception.ChallengeNotFoundException;
import com.mycom.myapp.common.exception.SettlementAlreadyDoneException;
import com.mycom.myapp.common.exception.UserNotFoundException;
import com.mycom.myapp.point.entity.PointHistory;
import com.mycom.myapp.point.entity.PointType;
import com.mycom.myapp.point.repository.PointHistoryRepository;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementServiceImpl implements SettlementService {

	private final PointService pointService;			// 포인트 기능 재사용
	private final UserRepository userRepository;
	private final PointHistoryRepository pointHistoryRepository;
	private final ChallengeRepository challengeRepository;
	private final ParticipationRepository participationRepository;

	@Override
	// #0. 전원 실패 시
	@Transactional
	public void penaltyAll(Long challengeId) {
		// 1. 챌린지 조회
		Challenge challenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new ChallengeNotFoundException(challengeId));

		// 2. 이미 정산된 챌린지 인지 확인
		if (challenge.getStatus() == ChallengeStatus.CLOSED) {
			throw new SettlementAlreadyDoneException(challengeId);
		}

		// 3. 참여자 목록 조회
		List<Participation> participants = participationRepository.findByChallenge_Id(challengeId);

		// 4. 각 참여자한테 보증금 전액 몰수
		for (Participation participation : participants) {
			User user = userRepository.findById(participation.getUser().getUserId())
					.orElseThrow(() -> new UserNotFoundException(participation.getUser().getUserId()));

			int deposit = challenge.getDepositAmount();
			user.setPointBalance(user.getPointBalance() - deposit);

			PointHistory history = new PointHistory(
					null,
					user,
					participation,						// 참여 정보 연결
					deposit,
					PointType.PENALTY,
					user.getPointBalance(),
					LocalDateTime.now()
			);
			pointHistoryRepository.save(history);
		}

		// 5. 챌린지 상태 CLOSED 변경
		challenge.setStatus(ChallengeStatus.CLOSED);
		challengeRepository.save(challenge);
	}

	@Override
	// #1. 환불 처리 (DEPOSIT_REFUND)
	@Transactional
	public void refund(Long userId, Long participationId, int amount) {
		log.info("환불 처리 시작 : userId = {}, paritipaitonId = {}, amount = {}", userId, participationId, amount);

		// 1. 유저 조회
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));

		// 2. 참여 정보 조회
		Participation participation = participationRepository.findById(participationId)
				.orElseThrow(() -> new IllegalArgumentException("참여 정보를 찾을 수 없습니다."));

		// 3. 유저 잔액 갱신
		user.setPointBalance(user.getPointBalance() + amount);

		// 4. 포인트 이력 저장 (DEPOSIT_REFUND 타입)
		PointHistory history = new PointHistory(
				null,
				user,
				participation,						// 참여 정보 연결
				amount,
				PointType.DEPOSIT_REFUND,
				user.getPointBalance(),
				LocalDateTime.now()
		);
		pointHistoryRepository.save(history);

		log.info("환불 처리 완료 : userId = {}, amount = {}", userId, amount);
	}

	@Override
	// #2. 몰수 처리 (PENALTY)
	@Transactional
	public void penalty(Long userId, Long participationId, int amount) {


		log.info("몰수 처리 시작 : userId = {}, paritipaitonId = {}, amount = {}", userId, participationId, amount);

		// 1. 유저 조회
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));

		// 2. 참여 정보 조회
		Participation participation = participationRepository.findById(participationId)
				.orElseThrow(() -> new IllegalArgumentException("참여 정보를 찾을 수 없습니다."));

		// 3. 참여 정보 불러오기
		Challenge challenge = participation.getChallenge();

		// 4. 유저 잔액 갱신
		user.setPointBalance(user.getPointBalance() - amount);

		// 5. 포인트 이력 저장 (PENALTY 타입)
		PointHistory history = new PointHistory(
				null,
				user,
				participation,						// 참여 정보 연결
				amount,
				PointType.PENALTY,
				user.getPointBalance(),
				LocalDateTime.now()
		);
		pointHistoryRepository.save(history);

		log.info("몰수 처리 완료 : userId = {}, amount = {}", userId, amount);

		// 5. 챌린지 상태 CLOSED 변경
		challenge.setStatus(ChallengeStatus.CLOSED);
		challengeRepository.save(challenge);
	}

	@Override
	// #3. 분배 처리 (REWARD)
	@Transactional
	public void reward(Long userId, Long participationId, int totalPenaltyAmount, int successCount) {

		// 0. 챌린지 상태 확인
		Participation participation = participationRepository.findById(participationId)
				.orElseThrow(() -> new IllegalArgumentException("참여 정보를 찾을 수 없습니다."));

		Challenge challenge = participation.getChallenge();

		// 챌린지 상태가 이미 CLOSED 라면 정산 불가
		if (challenge.getStatus() == ChallengeStatus.CLOSED) {
			throw new SettlementAlreadyDoneException(challenge.getId());
		}

		// 1. 1인당 분배 금액 계산 ( 소수점 버림 등 정책에 따라 조절 )
		int rewardAmount = totalPenaltyAmount / successCount;

		// 2. 유저 조회
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));

		// 3. 유저 잔액 갱신
		user.setPointBalance(user.getPointBalance() + rewardAmount);

		// 4. 포인트 이력 저장
		PointHistory history = new PointHistory(
				null,
				user,
				participation,
				rewardAmount,
				PointType.REWARD,
				user.getPointBalance(),
				LocalDateTime.now()
		);
		pointHistoryRepository.save(history);

		log.info("분배 처리 완료 : userId = {}, 지급액 = {}", userId, rewardAmount);

		// 5. 챌린지 상태 CLOSED 변경
		challenge.setStatus(ChallengeStatus.CLOSED);
		challengeRepository.save(challenge);
	}

	// #4. 챌린지 정산(settleChallenge)
	@Override
	public void settleChallenge(Long challengeId, Long hostId) {
		// 1. 챌린지 조회
		Challenge challenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new ChallengeNotFoundException(challengeId));
		
		// 2. 호스트 본인 확인
		if(!challenge.getHost().getUserId().equals(hostId)) {
			throw new AccessDeniedException("호스트만 정산할 수 있습니다.");
		}
		
		// 3. 이미 정산된 챌린지인지 확인
		if(challenge.getSettlementStatus() == SettlementStatus.SETTLED) {
			throw new SettlementAlreadyDoneException(challengeId);
		}
		
		// 4. 참여자 목록 조회
		List<Participation> participants = participationRepository.findByChallenge_Id(challengeId);
		
		// 5. 성공자 / 실패자 판정
		int successCount = 0;
		int totalPenaltyAmount = 0;
		
		for(Participation p : participants) {
			if(p.getSuccessCount() >= challenge.getRequiredCount()) {
				successCount++;												// 성공자 수 카운트
			} else {
				totalPenaltyAmount += challenge.getDepositAmount();			// 실패자 보증금 누적
			}
		}
		
		// 6. 정산 처리
		if(successCount == 0) {
			penaltyAll(challengeId);
		} else {
			for (Participation p : participants) {
				if(p.getSuccessCount() >= challenge.getRequiredCount()) {
					refund(p.getUser().getUserId(), p.getId(), challenge.getDepositAmount());
					reward(p.getUser().getUserId(), p.getId(), totalPenaltyAmount, successCount);
				} else {
					penalty(p.getUser().getUserId(), p.getId(), challenge.getDepositAmount());
				}
			}
		}
		
		// 7. 챌린지 상태 변경
		challenge.setStatus(ChallengeStatus.CLOSED);
		challenge.setSettlementStatus(SettlementStatus.SETTLED);
		challengeRepository.save(challenge);
	}
	
	// #5. 챌린지별 정산 금액 조회
	@Override
	public int getSettlementAmount(Long challengeId) {
		List<PointType> types = List.of(PointType.PENALTY, PointType.REWARD, PointType.DEPOSIT_REFUND);
		Integer amount = pointHistoryRepository.sumAmountByParticipation_Challenge_IdAndTypeIn(challengeId, types);
		return (amount != null) ? amount : 0;
	}
}
