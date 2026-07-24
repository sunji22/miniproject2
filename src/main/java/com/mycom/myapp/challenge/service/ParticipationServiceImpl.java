package com.mycom.myapp.challenge.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.dto.MyParticipationResponseDto;
import com.mycom.myapp.challenge.dto.ParticipantResponseDto;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.entity.ParticipationStatus;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.common.exception.ChallengeNotFoundException;
import com.mycom.myapp.common.exception.DuplicateParticipationException;
import com.mycom.myapp.common.exception.InsufficientPointException;
import com.mycom.myapp.common.exception.InvalidChallengeStatusException;
import com.mycom.myapp.common.exception.NotParticipationOwnerException;
import com.mycom.myapp.common.exception.ParticipationNotFoundException;
import com.mycom.myapp.common.exception.UserNotFoundException;
import com.mycom.myapp.point.service.PointService;
import com.mycom.myapp.point.service.SettlementService;
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
	
	private final SettlementService settlementService;
	
	/**
	 * 등록 = 챌린지 참여
	 * log.warn + throw new = 안티패턴 (테스트 확인용)
	 */
	@Override
	@Transactional
	public Long participate(Long challengeId, Long userId) {
		// 조회 & 영속화
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		Challenge challenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new ChallengeNotFoundException(challengeId));
		Optional<Participation> optionalParticipation = 
	            participationRepository.findByChallenge_IdAndUser_UserId(challengeId, userId);
		
		// [중복 참여 1차 방어]	
		if(optionalParticipation.isPresent()) {
			Participation existingParticipation = optionalParticipation.get();
	        // 취소했던 참여가 아니면 중복 예외 발생
	        if (existingParticipation.getStatus() != ParticipationStatus.CANCLED) {
	            log.warn("[참여 실패] 이미 참여 중인 챌린지 - challengeId: {}, userId: {}", challengeId, userId);
	            throw new DuplicateParticipationException();
	        }
		}
		
		// [Challenge 모집 상태 검증]: 모집중 상태만 참여 가능
		if(challenge.getStatus() != ChallengeStatus.RECRUITING) {
			log.warn("[참여 실패] 모집중이 아니다 - challengeId: {}", challengeId);
			throw new InvalidChallengeStatusException("모집 중인 챌린지만 참여할 수 있습니다.");
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
		
		// 참여 엔티티 처리 로직 : 신규 생성 or 상태 변경(CANCLED -> JOINED)
		Participation participation;
		
		// (case A) 참여했다가 취소한 경우 : CANCLED -> JOINED
		if(optionalParticipation.isPresent()) {
			participation = optionalParticipation.get();
			participation.rejoin();
			log.info("[재참여 성공] user {} 챌린지 {} 재참여(상태 변경)", userId, challengeId);
		
		// (case B) 신규 참여 : 새로운 참여 insert
		} else {
			participation = Participation.createParticipation(user, challenge);
			// [중복 참여 2차 방어]
			try {			
				participation = participationRepository.save(participation);
				// (동시성 이슈) 찰나에 2번 참여하기 하면 DB에 insert 가 2번 도착한다.
				// 2번째 insert 시에 UNIQUE 제약조건에 의해 DataIntegrityViolationException 발생
			} catch (DataIntegrityViolationException e) {
				log.error("[DB ERROR DETAILED REASON] ", e.getRootCause());
				throw new DuplicateParticipationException();
			}
			log.info("[신규 참여 성공] user {} 챌린지 {} 최초 참여", userId, challengeId);
		}
		
		// 식별자(PK)만 리턴
		return participation.getId();
	}	
	
	/**
	 * 참여자 목록 조회
	 * 자동생성 JPQL 사용 시 user.getUsername() 시 N+1 발생
	 * -> join fetch user 적용
	 * 응답용 DTO 리스트로 반환
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ParticipantResponseDto> listParticipant(Long challengeId) {
		List<ParticipantResponseDto> result = new ArrayList<>();
		// 참여 상태인 참여자만 조회
		List<Participation> participations = participationRepository
				.findByChallenge_IdAndStatus(challengeId, ParticipationStatus.JOINED);
		
		// Participation -> DTO 변환 후 리스트 추가
		// [N+1 발생 체크] 변환 시 participation.getUser().getName() 호출할 때.
		participations.forEach( p -> result.add(ParticipantResponseDto.from(p)));
		
		return result;
	}
	
	/**
	 * 유저의 참여 챌린지 목록 조회
	 * 위와 동일
	 */
	@Override
	@Transactional(readOnly = true)
	public List<MyParticipationResponseDto> listMyParticipation(Long userId) {
		List<MyParticipationResponseDto> result = new ArrayList<>();
		List<Participation> participations = participationRepository.findByUser_UserId(userId);
		
		// 변환. [N+1 발생 체크]
		participations.forEach( p -> result.add(MyParticipationResponseDto.from(p)) );
		
		return result;
	}
	
	// 참여 정보 1건 조회
	@Override
	public Participation detailParticipation(Long userId, Long challengeId) {
		return participationRepository.findByChallenge_IdAndUser_UserId(challengeId, userId)
								.orElseThrow(() -> new ParticipationNotFoundException(challengeId));	 
	}
	
	
	/**
	 * 참여 취소 = 참여 삭제
	 * @return 반환된 보증금 금액
	 */
	@Override
	@Transactional
	public int deleteParticipation(Long participationId, Long userId) {
		Participation participation = participationRepository.findById(participationId)
				.orElseThrow(() -> new ParticipationNotFoundException(participationId));
		
		// 1. 요청자 = 참여자 검증 (403 FORBIDDEN)
		Long participatedUserId = participation.getUser().getUserId();
		if(!userId.equals(participatedUserId)) {
			throw new NotParticipationOwnerException(); 
		}
		
		// 2. 모집중 상태 검증 : 모집중(시작 전) 일 때만 취소 가능 (400 BAD_REQUEST) 
		if(participation.getChallenge().getStatus() != ChallengeStatus.RECRUITING) {
			throw new InvalidChallengeStatusException();
		}
		
		// 3. 보증금 반환
		int depositAmount = participation.getChallenge().getDepositAmount();
		settlementService.refund(userId, participationId, depositAmount);
		
		// 4. 삭제 : 상태만 변경 (JOINED -> CANCLED)
		// 모집중 검증을 지나왔으므로 참여 상태는 JOINED 만 존재
		participation.cancel();
		
		return depositAmount;
	}
}
