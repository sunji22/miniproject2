package com.mycom.myapp.challenge.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.domain.SettlementStatus;
import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.entity.ParticipationStatus;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.common.exception.CannotDeleteOngoingChallengeException;
import com.mycom.myapp.common.exception.ChallengeNotFoundException;
import com.mycom.myapp.common.exception.ExceededRequiredCountException;
import com.mycom.myapp.common.exception.InvalidChallengePeriodException;
import com.mycom.myapp.common.exception.InvalidChallengeStatusException;
import com.mycom.myapp.common.exception.NotChallengeHostException;
import com.mycom.myapp.common.exception.ParticipationNotFoundException;
import com.mycom.myapp.common.exception.UserNotFoundException;
import com.mycom.myapp.point.service.SettlementService;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

	private final ChallengeRepository challengeRepository;
	private final UserRepository userRepository; // -> UserService 로 결합도 낮추기 ?
	private final ParticipationRepository participationRepository;
	
	private final ParticipationService participationService;
	private final SettlementService settlementService;		// 삭제 시 보증금 환불용

	// 우선 status 필터링만
	@Override
	public List<ChallengeDto> listChallenge(ChallengeSearchConditionDto conditionDto) {
		
		Pageable pageable = PageRequest.of(
				conditionDto.getPage(), 
				conditionDto.getSize(),
				// 기본 정렬: 최신순
				Sort.by(Sort.Direction.DESC, "createdAt")
		);
		Page<Challenge> page = null;
		
		// status 조건 있으면
		if(conditionDto.getStatus() != null) {
			page = challengeRepository.findByStatus(conditionDto.getStatus(), pageable);
		// status 없으면 전체 조회
		} else {
			page = challengeRepository.findByStatusNot(ChallengeStatus.DELETED, pageable);
		}
		
		List<ChallengeDto> challengeDtoList = new ArrayList<>();
		
		// page -> List
		page.toList().forEach( challenge -> {
			challengeDtoList.add(ChallengeDto.from(challenge));
		});
		
		return challengeDtoList;
	}

	// 상세 조회
	@Override
	public ChallengeDto detailChallenge(Long id, Long userId) {
		// DELETED 상태이면 404 NotFound 
		Challenge challenge = challengeRepository.findByIdAndStatusNot(id, ChallengeStatus.DELETED)
									.orElseThrow(() -> new ChallengeNotFoundException(id));
		ChallengeDto challengeDto = ChallengeDto.from(challenge);
		
		// [ 사용자에 따라 참여 id 세팅하기 ]
		
		// 1. 미인증 사용자 (userId == null)
		if(userId == null) {
			challengeDto.setParticipationId(null);
	        return challengeDto;
		}
		
		// 2. 인증 사용자 - 참여 id
		Long participationId = participationRepository
	            .findByChallenge_IdAndUser_UserIdAndStatus(id, userId, ParticipationStatus.JOINED)
	            .map(Participation::getId)
	            // 미참여자 = null
	            .orElse(null);
		challengeDto.setParticipationId(participationId);
		
		return challengeDto;
	}

	@Override
	@Transactional
	public Long insertChallenge(ChallengeDto challengeDto, Long userId) {
		log.info("Service 진입");
		// 1. 날짜 유효성 검증 (시작일이 종료일보다 늦은 경우 차단)
	    if (challengeDto.getStartDate().isAfter(challengeDto.getEndDate())) {
	        throw new InvalidChallengePeriodException(); // "시작일은 종료일보다 이전이어야한다."
	    }
		
		// 2.required_count 가 전체 기간보다 큰 경우 -> 예외 추가 필요
		long totalDays = ChronoUnit.DAYS.between(challengeDto.getStartDate(), challengeDto.getEndDate()) + 1;
		if(challengeDto.getRequiredCount() > totalDays) {
			// "인증 필요 횟수가 총 챌린지 기간(일)을 초과할 수 없습니다."
			throw new ExceededRequiredCountException(totalDays);
		}

		// 3. 주최사 User 영속화 조회
		User user = userRepository.findById(userId)
						.orElseThrow(() -> new UserNotFoundException(userId));
		
		log.info("유저 조회 완료: userId={}", user.getName());
		
		// 4. 챌린지 엔티티 생성 및 저장/영속화
		Challenge challenge = challengeDto.toEntity(user);
		challengeRepository.save(challenge);
		
		// 5. 챌린지 개설자는 개설과 동시에 자동 참여 (단일 트랜잭션 보장 - 보증금 차감 실패 시 챌린지 생성도 롤백)
		participationService.participate(challenge.getId(), userId);

		// 당일을 시작일로 정한 챌린지는 바로 '진행중'으로 변경
		if(LocalDate.now().equals(challenge.getStartDate())) {
			challenge.setStatus(ChallengeStatus.ONGOING);
		}
		
		return challenge.getId();
	}

	@Override
	@Transactional
	public Long updateChallenge(ChallengeDto challengeDto, Long userId) {
		/*
		 * 검증
		 * 1. required_count(최소인증횟수) 가 전체 기간보다 큰 경우
		 * 2. 해당 요청자가 원래의 작성자(host_id) 인지
		 * 3. 이미 진행 중인 챌린지는 수정 불가
		 * 4. 참여자가 1명이라도 있으면, 수정 필드를 제한 (핵심 정보 수정 불가)
		 */
		// Bad Request
		if(challengeDto.getId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "챌린지의 ID가 없습니다.");
		}
		Challenge challenge = challengeRepository
								.findById(challengeDto.getId())
								.orElseThrow(() -> new ChallengeNotFoundException(challengeDto.getId()));

		// (검증1) 최소인증횟수 검증
		long totalDays = ChronoUnit.DAYS.between(challengeDto.getStartDate(), challengeDto.getEndDate()) + 1;
		if(challengeDto.getRequiredCount() > totalDays) {
			throw new ExceededRequiredCountException(totalDays);
		}
		
		// (검증2) 요청자=작성자 검증
		Long hostId = challenge.getHost().getUserId(); // .getHost() 는 영속화된 User 를 참조
		if(!userId.equals(hostId)) { // userId=요청자
			throw new NotChallengeHostException();
		}
		
		// (검증3) 이미 진행 중인 챌린지
		ChallengeStatus status = challenge.getStatus();
		if(status != ChallengeStatus.RECRUITING) {
			throw new InvalidChallengeStatusException();
		}
		
		// (검증4) 주최자를 제외한 참여자가 1명이라도 있는 경우 핵심 조건(보증금, 일정, 인증 횟수) 수정 제한
		boolean hasOtherParticipants = participationRepository
				.existsByChallenge_IdAndUser_UserIdNotAndStatus(challenge.getId(), userId, ParticipationStatus.JOINED);

		if (hasOtherParticipants) {
			boolean isKeyFieldModified = ( challenge.getDepositAmount() != challengeDto.getDepositAmount() )
					|| ( challenge.getRequiredCount() != challengeDto.getRequiredCount() )
					|| !challenge.getStartDate().equals(challengeDto.getStartDate())
					|| !challenge.getEndDate().equals(challengeDto.getEndDate());

			if (isKeyFieldModified) {
				throw new CannotDeleteOngoingChallengeException("참여자가 존재하는 챌린지는 핵심 조건(보증금, 일정, 인증 횟수)을 수정할 수 없습니다.");
			}
		}
		
		log.info("업데이트 전: {}", challenge.getTitle());
		
		// Dirty Checking
		challenge.setTitle(challengeDto.getTitle());
		challenge.setDescription(challengeDto.getDescription());
		challenge.setDepositAmount(challengeDto.getDepositAmount());
		challenge.setRequiredCount(challengeDto.getRequiredCount());
		challenge.setStartDate(challengeDto.getStartDate());
		challenge.setEndDate(challengeDto.getEndDate());
		
		log.info("업데이트 후: {}", challenge.getTitle());
				
		return challenge.getId();
	}

	@Override
	@Transactional
	public void deleteChallenge(Long challengeId, Long userId) {
		Challenge challenge = challengeRepository
								.findById(challengeId)
								.orElseThrow(() -> new ChallengeNotFoundException(challengeId));
		// (검증1) 요청자=작성자
		Long hostId = challenge.getHost().getUserId();
		if(!userId.equals(hostId)) { // userId=요청자
			throw new NotChallengeHostException();
		}
		
		// (검증2) 진행중 챌린지 삭제 불가
		if(challenge.getStatus() == ChallengeStatus.ONGOING) {
			throw new CannotDeleteOngoingChallengeException();
		}
		
		// (검증3) 주최자 외의 참여자가 있으면 삭제 불가능
		boolean hasOtherParticipants = participationRepository
				.existsByChallenge_IdAndUser_UserIdNotAndStatus(challengeId, userId, ParticipationStatus.JOINED);
		if (hasOtherParticipants) {
			throw new CannotDeleteOngoingChallengeException("다른 참여자가 존재하여 챌린지를 삭제할 수 없습니다.");
		}
		
		// 주최자 외의 참여자가 없으면 삭제
		
		// 자식 테이블 먼저 논리 삭제
		Participation hostParticipation = participationRepository
				.findByChallenge_IdAndUser_UserId(challengeId, userId)
				.orElseThrow(ParticipationNotFoundException::new);

		// 참여(JOINED)+미정산이면 개설자 보증금 잠금 해제(환불) 후 취소
		if (hostParticipation.getStatus() == ParticipationStatus.JOINED
				&& challenge.getSettlementStatus() != SettlementStatus.SETTLED) {
			settlementService.refund(userId, hostParticipation.getId(), challenge.getDepositAmount());
		}
		hostParticipation.cancel();
		// 부모 챌린지 논리 삭제
		challenge.delete();
		
		log.info("챌린지 {} 삭제", challenge.getId());
	}
	
	// 타 도메인에서 사용할 '유효성 검증이 완료된 Challenge 엔티티 반환 메소드'
	public Challenge getValidChallenge(Long id) {
		Challenge challenge = challengeRepository.findById(id)
				.orElseThrow(() -> new ChallengeNotFoundException(id));
		return challenge;
	}
}
