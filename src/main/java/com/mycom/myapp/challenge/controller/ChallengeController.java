package com.mycom.myapp.challenge.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.service.ChallengeService;
import com.mycom.myapp.challenge.service.ParticipationService;
import com.mycom.myapp.common.ResultDto;
import com.mycom.myapp.config.MyUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

	private final ChallengeService challengeService;
	private final ParticipationService participationService;
	
	@GetMapping
	public ResultDto<List<ChallengeDto>> listChallenge(
			ChallengeSearchConditionDto searchCondition) {
		// ChallengeStatus 가 아닌 문자열 입력 시 HttpMessageNotReadableException
		return challengeService.listChallenge(searchCondition);
	}
	
	@GetMapping("/{id}")
	public ResultDto<ChallengeDto> detailChallenge(@PathVariable("id") Long id) {
		return challengeService.detailChallenge(id);
	}
	
	@PostMapping
	public ResultDto<Long> insertChallenge(
			@Valid @RequestBody ChallengeDto challengeDto) {
		return challengeService.insertChallenge(challengeDto);
	}

	@PutMapping
	public ResultDto<Long> updateChallenge(@Valid ChallengeDto challengeDto) {
		return challengeService.insertChallenge(challengeDto);
	}
	
	@DeleteMapping
	public ResultDto<Void> deleteChallenge(Long id) {
		return challengeService.deleteChallenge(id);
	}
	
	
	//////////////  챌린지 참여  ////////////////
	
	// data = 참여 챌린지 id
	@PostMapping("/{id}/participations")
	public ResultDto<Long> participate(
				@PathVariable("id") Long challengeId,
				// SecurityContextHolder 에서 MyUserDetails 의 id 만 추출
				// MyUserDetails 의 getter 이름이랑 expression 문자열이 대응해야 함.
				@AuthenticationPrincipal(expression = "id") Long userId
			) {
		Long savedId = participationService.participate(challengeId, userId);
		
		return ResultDto.success(savedId);
	}
}
