package com.mycom.myapp.point.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
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
public class SettlementService {
	
	private final Participation participation;
	private final PointService pointService;			// 포인트 기능 재사용
	private final UserRepository userRepository;
	private final PointHistoryRepository pointHistoryRepository;
	private final ParticipationRepository participationRepository;
	
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
		
		// 3. 유저 잔액 갱신
		user.setPointBalance(user.getPointBalance() - amount);
		
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
	
	// #3. 분배 처리 (REWARD)
	@Transactional
	public void reward(Long userId, Long participationId, int totalPenaltyAmount, int successCount) {
		
		// 1. 1인당 분배 금액 계산 ( 소수점 버림 등 정책에 따라 조절 )
		int rewardAmount = totalPenaltyAmount / successCount;
		
		// 2. 유저 조회
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		
		// 3. 유저 잔액 갱신
		user.setPointBalance(user.getPointBalance() - rewardAmount);
		
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
	}
}
