package com.mycom.myapp.challenge.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

	@Override
	public ResultDto<Long> insertChallenge(ChallengeDto challengeDto) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultDto<Long> updateChallenge(ChallengeDto challengeDto) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultDto<Void> deleteChallenge(Long id) {
		// TODO Auto-generated method stub
		return null;
	}
}
