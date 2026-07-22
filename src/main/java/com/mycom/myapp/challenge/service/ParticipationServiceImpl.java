package com.mycom.myapp.challenge.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.common.exception.DuplicateParticipationException;
import com.mycom.myapp.common.exception.InsufficientPointException;
import com.mycom.myapp.common.exception.UserNotFoundException;
import com.mycom.myapp.point.service.PointService;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParticipationServiceImpl implements ParticipationService{

	private final ParticipationRepository parciParticipationRepository;
	private final UserRepository userRepository; // -> 이후 Service 를 주입받도록 개선 필요
	private final ChallengeService challengeService;
	private final PointService pointService;
	
	// 등록 = 챌린지 참여
	@Override
	@Transactional
	public Long participate(Long challengeId, Long userId) {
		// [중복 참여 1차 방어]
		if(parciParticipationRepository.existsByChallenge_IdAndUser_UserId(challengeId, userId)) {
			throw new DuplicateParticipationException();
		}
		
		// 조회 & 영속화
		User user = userRepository.findById(userId)
						.orElseThrow(() -> new UserNotFoundException(userId));
		Challenge challenge = challengeService.getValidChallenge(challengeId);
		
		// [Challenge 모집 상태 검증]: 모집중 상태만 참여 가능
		if(challenge.getStatus() != ChallengeStatus.RECRUITING) {
			// 예외 추가 필요
			throw new RuntimeException();
		}
		
		// [User 잔액 검증]
		int depositAmount = challenge.getDepositAmount();
		int balance = user.getPointBalance();
		if(balance < depositAmount) {
			throw new InsufficientPointException(depositAmount, balance);
		}

		// 보증금 잠금
		pointService.lockPoint(userId, depositAmount);
		
		// 참여 엔티티 객체 생성 -> 저장
		Participation participation = Participation.createParticipation(user, challenge);
		// [중복 참여 2차 방어]
		try {			
			parciParticipationRepository.save(participation);
		// 찰나에 2번 참여하기 하면 DB에 insert 가 2번 도착한다.
		// 2번째 insert 시에 UNIQUE 제약조건에 의해 DataIntegrityViolationException 발생
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateParticipationException();
		}
		
		// 식별자(PK)만 리턴
		return participation.getId();
	}
}
