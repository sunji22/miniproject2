package com.mycom.myapp.challenge.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.dto.MyParticipationResponseDto;
import com.mycom.myapp.challenge.dto.ParticipantResponseDto;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.common.exception.ChallengeNotFoundException;
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

	private final ParticipationRepository participationRepository;
	private final UserRepository userRepository; // -> 이후 Service 를 주입받도록 개선 필요 ?
	private final ChallengeRepository challengeRepository; // 순환참조 때문에 직접 주입받도록 수정
	private final PointService pointService;
	
	/**
	 * 등록 = 챌린지 참여
	 * log.warn + throw new = 안티패턴 (테스트 확인용)
	 */
	@Override
	@Transactional
	public Long participate(Long challengeId, Long userId) {
		// [중복 참여 1차 방어]
		if(participationRepository.existsByChallenge_IdAndUser_UserId(challengeId, userId)) {
			log.warn("[참여 실패] 중복 참여1 - challengeId: {}, userId: {}", challengeId, userId);
			throw new DuplicateParticipationException();
		}
		
		// 조회 & 영속화
		User user = userRepository.findById(userId)
						.orElseThrow(() -> new UserNotFoundException(userId));
		Challenge challenge = challengeRepository.findById(challengeId)
						.orElseThrow(() -> new ChallengeNotFoundException(challengeId));
		
		// [Challenge 모집 상태 검증]: 모집중 상태만 참여 가능
		if(challenge.getStatus() != ChallengeStatus.RECRUITING) {
			log.warn("[참여 실패] 모집중이 아니다 - challengeId: {}", challengeId);
			// 예외 추가 필요
			throw new RuntimeException();
		}
		
		// [User 잔액 검증]
		int depositAmount = challenge.getDepositAmount();
		int balance = user.getPointBalance();
		if(balance < depositAmount) {
			log.warn("[참여 실패] 잔액 부족 - userId: {}, depositAmount: {}, balance: {}", userId, depositAmount, balance);
			throw new InsufficientPointException(depositAmount, balance);
		}

		// 잔액 검증 후 보증금 잠금
		pointService.lockPoint(userId, depositAmount);
		log.info("[보증금 잠금 성공] user {} 보증금 {} 잠금", userId, depositAmount);
		
		// 참여 엔티티 객체 생성 -> 저장
		Participation participation = Participation.createParticipation(user, challenge);
		// [중복 참여 2차 방어]
		try {			
			participationRepository.save(participation);
		// 찰나에 2번 참여하기 하면 DB에 insert 가 2번 도착한다.
		// 2번째 insert 시에 UNIQUE 제약조건에 의해 DataIntegrityViolationException 발생
		} catch (DataIntegrityViolationException e) {
//			log.warn("[참여 실패] 중복 참여2 - challengeId: {}, userId: {}", challengeId, userId);
			log.error("[DB ERROR DETAILED REASON] ", e.getRootCause());
			throw new DuplicateParticipationException();
		}
		
		log.info("[참여 성공] user {} 챌린지 {} 참여", userId, challengeId);
		// 식별자(PK)만 리턴
		return participation.getId();
	}
	
	/**
	 * 참여자 목록 조회
	 * 자동생성 JPQL 사용 시 user.getUsername() 시 N+1 발생
	 * -> join fetch user 적용
	 * 응답용 DTO 리스트로 반환
	 */
	@Transactional(readOnly = true)
	public List<ParticipantResponseDto> listParticipant(Long challengeId) {
		List<ParticipantResponseDto> result = new ArrayList<>();
		List<Participation> participations = participationRepository.findByChallenge_Id(challengeId);
		
		// Participation -> DTO 변환 후 리스트 추가
		// [N+1 발생 체크] 변환 시 participation.getUser().getName() 호출할 때.
		participations.forEach( p -> result.add(ParticipantResponseDto.from(p)));
		
		return result;
	}
	
	/**
	 * 유저의 참여 챌린지 목록 조회
	 * 위와 동일
	 */
	@Transactional(readOnly = true)
	public List<MyParticipationResponseDto> listMyParticipation(Long userId) {
		List<MyParticipationResponseDto> result = new ArrayList<>();
		List<Participation> participations = participationRepository.findByUser_UserId(userId);
		
		// 변환. [N+1 발생 체크]
		participations.forEach( p -> result.add(MyParticipationResponseDto.from(p)) );
		
		return null;
	}
}
