package com.mycom.myapp.point.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.domain.SettlementStatus;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.entity.ParticipationStatus;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.common.exception.ChallengeNotFoundException;
import com.mycom.myapp.common.exception.ParticipationNotFoundException;
import com.mycom.myapp.common.exception.SettlementAlreadyDoneException;
import com.mycom.myapp.common.exception.UserNotFoundException;
import com.mycom.myapp.point.dto.SettlementPreviewResponseDto;
import com.mycom.myapp.point.dto.SettlementPreviewResponseDto.ParticipantPreview;
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
	// #0. 전원 실패 시 (penatly-all)
	@Transactional
	public void penaltyAll(Long challengeId) {
		// 1. 챌린지 조회
		Challenge challenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new ChallengeNotFoundException(challengeId));

		// 2. 이미 정산된 챌린지 인지 확인 (중복 정산 방지)
		if (challenge.getSettlementStatus() == SettlementStatus.SETTLED) {
			throw new SettlementAlreadyDoneException(challengeId);
		}

		// 3. 참여자 목록 조회
		List<Participation> participants = participationRepository.findByChallenge_IdAndStatus(challengeId, ParticipationStatus.JOINED);

		// 4. 각 참여자한테 보증금 전액 몰수
		for (Participation participation : participants) {
			User user = userRepository.findById(participation.getUser().getUserId())
					.orElseThrow(() -> new UserNotFoundException(participation.getUser().getUserId()));

			int deposit = challenge.getDepositAmount();

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
				.orElseThrow(() -> new ParticipationNotFoundException(participationId));
		
		// 3. 정산 여부 확인 (중복 정산 방지)
		Challenge challenge = participation.getChallenge();
		if(challenge.getSettlementStatus() == SettlementStatus.SETTLED) {
			throw new SettlementAlreadyDoneException(challenge.getId());
		}

		// 4. 유저 잔액 갱신
		user.setPointBalance(user.getPointBalance() + amount);

		// 5. 포인트 이력 저장 (DEPOSIT_REFUND 타입)
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
				.orElseThrow(() -> new ParticipationNotFoundException(participationId));

		// 3. 챌린지 조회 + 정산 여부 확인 (중복 정산 방지)
		Challenge challenge = participation.getChallenge();
		if(challenge.getSettlementStatus() == SettlementStatus.SETTLED) {
			throw new SettlementAlreadyDoneException(challenge.getId());
		}


		// 4. 포인트 이력 저장 (PENALTY 타입)
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
	}

	@Override
	// #3. 분배 처리 (REWARD)
	@Transactional
	public void reward(Long userId, Long participationId, int totalPenaltyAmount, int successCount) {

		// 1. 참여 정보 조회
		Participation participation = participationRepository.findById(participationId)
				.orElseThrow(() -> new ParticipationNotFoundException(participationId));

		// 2. 챌린지 조회 + 정산 여부 확인 (중복 정산 방지)
		Challenge challenge = participation.getChallenge();
		if (challenge.getSettlementStatus() == SettlementStatus.SETTLED) {
			throw new SettlementAlreadyDoneException(challenge.getId());
		}

		// 3. 1인당 분배 금액 계산 ( 소수점 버림 등 정책에 따라 조절 )
		int rewardAmount = totalPenaltyAmount / successCount;

		// 4. 유저 조회
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));

		// 5. 유저 잔액 갱신
		user.setPointBalance(user.getPointBalance() + rewardAmount);

		// 6. 포인트 이력 저장
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
		
		// 3. 이미 정산된 챌린지인지 확인 (중복 정산 방지)
		if(challenge.getSettlementStatus() == SettlementStatus.SETTLED) {
			throw new SettlementAlreadyDoneException(challengeId);
		}
		
		// 4. 참여자 목록 조회
		List<Participation> participants = participationRepository.findByChallenge_IdAndStatus(challengeId, ParticipationStatus.JOINED);
		
		// 5. 성공자 / 실패자 판정
		int successCount = 0;
		int totalPenaltyAmount = 0;
		
		for(Participation p : participants) {
			if(p.getSuccessCount() >= challenge.getRequiredCount()) {
				successCount++;												// 성공자 수 카운트
				p.setStatus(ParticipationStatus.SUCCESS);					// 참여자 상태 성공으로 변경
			} else {
				totalPenaltyAmount += challenge.getDepositAmount();			// 실패자 보증금 누적
				p.setStatus(ParticipationStatus.FAILED);					// 참여자 상태 실패로 변경
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
		
		participationRepository.saveAll(participants);				// 참여자 상태 저장
		
		// #5. 챌린지 상태 변경 ( 각 정산을 통해 상태 변경 -> 한 번만 선언) 중복 제거
		challenge.setStatus(ChallengeStatus.CLOSED);
		challenge.setSettlementStatus(SettlementStatus.SETTLED);
		challengeRepository.save(challenge);
	}
	
	// #6. 챌린지별 정산 금액 조회
	@Override
	public int getSettlementAmount(Long challengeId) {
		List<PointType> types = List.of(PointType.PENALTY, PointType.REWARD, PointType.DEPOSIT_REFUND);
		Integer amount = pointHistoryRepository.sumAmountByParticipation_Challenge_IdAndTypeIn(challengeId, types);
		return (amount != null) ? amount : 0;
	}

	// #7. 정산 미리 보기 (읽기만 가능 - DB 변경 X)
	@Override
	@Transactional(readOnly=true)
	public SettlementPreviewResponseDto previewSettlement(Long challengeId, Long userId) {
		// 1. 챌린지 조회
		Challenge challenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new ChallengeNotFoundException(challengeId));
		
		// 2. 참여자 목록 조회
		List<Participation> participants = participationRepository.findByChallenge_IdAndStatus(challengeId, ParticipationStatus.JOINED);
		
		// 3. 참여자 확인
		boolean isParticipant = participants.stream().anyMatch(p -> p.getUser().getUserId().equals(userId));
		
		if (!isParticipant) {
			throw new AccessDeniedException("참여자만 정산 미리보기를 볼 수 있습니다.");
		}
		
		// 4. 성공자 / 실패자 판정
		int successCount = 0;
		int totalPenaltyAmount = 0;
		
		for (Participation p : participants) {
			if(p.getSuccessCount() >= challenge.getRequiredCount()) {
				successCount++;
			} else {
				totalPenaltyAmount += challenge.getDepositAmount();
			}
		}
		
		int failCount = participants.size() - successCount;
		int rewardPerPerson = (successCount > 0) ? totalPenaltyAmount / successCount : 0;
		
		// 5. 참여자별 미리보기 계산
		List<ParticipantPreview> previewList = new ArrayList<>();
		
		for(Participation p : participants) {
			User user = userRepository.findById(p.getUser().getUserId())
					.orElseThrow(() -> new UserNotFoundException(p.getUser().getUserId()));
			
			boolean isSuccess = p.getSuccessCount() >= challenge.getRequiredCount();
			int currentBalance = user.getPointBalance();
			
			int refundAmount = 0;
			int rewardAmount = 0;
			int penaltyAmount = 0;
			int expectedBalance = currentBalance;
			
			if (isSuccess) {
				refundAmount = challenge.getDepositAmount();
				rewardAmount = rewardPerPerson;
				expectedBalance = currentBalance + refundAmount + rewardAmount;
			} else { 
				penaltyAmount = challenge.getDepositAmount();
				expectedBalance = currentBalance;
			}
			
			previewList.add(ParticipantPreview.builder()
					.userId(user.getUserId())
					.userName(user.getName())
					.currentSuccessCount(p.getSuccessCount())
					.success(isSuccess)
					.refundAmount(refundAmount)
					.rewardAmount(rewardAmount)
					.penaltyAmount(penaltyAmount)
					.currentBalance(currentBalance)
					.expectedBalance(expectedBalance)
					.build());
		}
		
		// 6. 최종 DTO 반환
		return SettlementPreviewResponseDto.builder()
				.challengeId(challengeId)
				.challengeTitle(challenge.getTitle())
				.depositAmount(challenge.getDepositAmount())
				.requiredCount(challenge.getRequiredCount())
				.totalParticipants(participants.size())
				.successCount(successCount)
				.failCount(failCount)
				.totalPenaltyAmount(totalPenaltyAmount)
				.rewardPerPerson(rewardPerPerson)
				.participants(previewList)
				.build();
	}
}
