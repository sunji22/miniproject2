package com.mycom.myapp.challenge.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.common.ResultDto;
import com.mycom.myapp.common.exception.ChallengeNotFoundException;
import com.mycom.myapp.common.exception.UserNotFoundException;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

	private final ChallengeRepository challengeRepository;
	private final UserRepository userRepository; // -> UserService 로 결합도 낮추기
	
	private final ParticipationService participationService;

	// 우선 status 필터링만
	@Override
	public ResultDto<List<ChallengeDto>> listChallenge(ChallengeSearchConditionDto conditionDto) {
		
		Pageable pageable = PageRequest.of(conditionDto.getPage(), conditionDto.getSize());
		Page<Challenge> page = null;
		
		// status 조건 있으면
		if(conditionDto.getStatus() != null) {
			page = challengeRepository.findByStatusOrderByCreatedAt(conditionDto.getStatus(), pageable);
		// status 없으면 전체 조회
		} else {
			page = challengeRepository.findAll(pageable);
		}
		
		List<ChallengeDto> challengeDtoList = new ArrayList<>();
		
		// page -> List
		page.toList().forEach( challenge -> {
			challengeDtoList.add(ChallengeDto.from(challenge));
		});
		
		return ResultDto.success(challengeDtoList);
	}

	// 상세 조회
	@Override
	public ResultDto<ChallengeDto> detailChallenge(Long id) {
		Challenge challenge = challengeRepository.findById(id)
									.orElseThrow(() -> new ChallengeNotFoundException(id));
		ChallengeDto challengeDto = ChallengeDto.from(challenge);
		
		return ResultDto.success(challengeDto);
	}

	@Override
	@Transactional
	public ResultDto<Long> insertChallenge(ChallengeDto challengeDto) {
		
		// required_count 가 전체 기간보다 큰 경우 -> 예외 추가 필요
		long totalDays = ChronoUnit.DAYS.between(challengeDto.getStartDate(), challengeDto.getEndDate()) + 1;
		if(challengeDto.getRequiredCount() > totalDays) {
			// 비즈니스 예외 추가 필요
			throw new RuntimeException();
		}

		// dto -> 엔티티 변환하기 위해서 영속화된 User 엔티티 필욧
		Long userId = challengeDto.getHostId();
		Long challengeId = challengeDto.getId();
		User user = userRepository.findById(userId)
						.orElseThrow(() -> new UserNotFoundException(userId));
		
		challengeDto.setCreatedAt(LocalDateTime.now()); // -> 리팩토링 필요할듯
		Challenge challenge = challengeRepository.save(challengeDto.toEntity(user));
		
		// 챌린지 개설자는 개설과 동시에 바로 참여
		participationService.participate(challengeId, userId);
		
		return ResultDto.success(challenge.getId());
	}

	@Override
	@Transactional
	public ResultDto<Long> updateChallenge(ChallengeDto challengeDto) {
		/*
		 * 검증
		 * 1. required_count(최소인증횟수) 가 전체 기간보다 큰 경우
		 * 2. 해당 요청자가 원래의 작성자(host_id) 인지
		 * 3. 이미 진행 중인 챌린지는 수정 불가
		 */
		Challenge existing = challengeRepository
								.findById(challengeDto.getId())
								.orElseThrow(() -> new ChallengeNotFoundException(challengeDto.getId()));

		// (검증1) 최소인증횟수 검증
		long totalDays = ChronoUnit.DAYS.between(challengeDto.getStartDate(), challengeDto.getEndDate()) + 1;
		if(challengeDto.getRequiredCount() > totalDays) {
			throw new RuntimeException();
		}
		
		// (검증2) 요청자=작성자 검증
		Long requesterId = existing.getHost().getUserId(); // User 엔티티 필요
		if(!challengeDto.getHostId().equals(requesterId)) {
			throw new RuntimeException();
		}
		
		// (검증3) 이미 진행 중인 챌린지
		ChallengeStatus status = existing.getStatus();
		if(status != ChallengeStatus.RECRUITING) {
			throw new RuntimeException();
		}
		
		// 검증 끝난 후 User 엔티티 영속화
		Long userId = challengeDto.getHostId();
		User user = userRepository.findById(userId)
						.orElseThrow(() -> new UserNotFoundException(userId));
		
		log.info("업데이트 전: {}", existing.getTitle());
		Challenge challenge = challengeRepository.save(challengeDto.toEntity(user));
		log.info("업데이트 후: {}", existing.getTitle());
				
		return ResultDto.success(challenge.getId());
	}

	@Override
	@Transactional
	public ResultDto<Void> deleteChallenge(Long id) {
		// 검증 : 진행중 챌린지 삭제 불가
		Challenge existing = challengeRepository
								.findById(id)
								.orElseThrow(() -> new ChallengeNotFoundException(id));
		if(existing.getStatus() == ChallengeStatus.ONGOING) {
			throw new RuntimeException();
		}
		
		// 삭제 시 필요한 추가적인 비즈니스 로직
		// ...
		
		challengeRepository.delete(existing);
		
		return ResultDto.success();
	}
	
	// 타 도메인에서 사용할 '유효성 검증이 완료된 Challenge 엔티티 반환 메소드'
	public Challenge getValidChallenge(Long id) {
		Challenge challenge = challengeRepository.findById(id)
				.orElseThrow(() -> new ChallengeNotFoundException(id));
		return challenge;
	}
}
