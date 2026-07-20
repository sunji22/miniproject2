package com.mycom.myapp.challenge.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.service.ChallengeService;
import com.mycom.myapp.common.ResultDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

	private final ChallengeService challengeService;
	
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
}
