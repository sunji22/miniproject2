package com.mycom.myapp.challenge.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.common.ResultDto;
import com.mycom.myapp.common.exception.ChallengeNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

	private final ChallengeRepository challengeRepository;

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

	// 챌린지 개설자는 바로 참여하게 되므로
	// (챌린지 등록) + (참여 등록) 트랜잭션 필요
	@Override
	@Transactional
	public ResultDto<Long> insertChallenge(ChallengeDto challengeDto) {
		// required_count 가 전체 기간보다 큰 경우 -> 예외 추가 필요

		challengeDto.setCreatedAt(LocalDateTime.now()); // -> 리팩토링 필요할듯
		Challenge challenge = challengeRepository.save(challengeDto.toEntity());
		
		// 참여 테이블에 등록 로직 추가
		
		return ResultDto.success(challenge.getId());
	}

	@Override
	@Transactional
	public ResultDto<Long> updateChallenge(ChallengeDto challengeDto) {
		// required_count 가 전체 기간보다 큰 경우 -> 예외 추가 필요
		
		// 해당 요청자가 원래의 작성자(host_id)가 맞는지 검증 필요

		// 이미 진행 중인 챌린지는 수정 불가. 검증 필요
		
		
		Challenge challenge = challengeRepository.save(challengeDto.toEntity());
		
		return ResultDto.success(challenge.getId());
	}

	@Override
	public ResultDto<Void> deleteChallenge(Long id) {
		// TODO Auto-generated method stub
		return null;
	}
}
