package com.mycom.myapp.challenge.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.dto.ParticipantResponseDto;
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
		List<ChallengeDto> challengeList = challengeService.listChallenge(searchCondition);
		return ResultDto.success(challengeList);
	}
	
	@GetMapping("/{id}")
	public ResultDto<ChallengeDto> detailChallenge(@PathVariable("id") Long id) {
		ChallengeDto challengeDto = challengeService.detailChallenge(id);
		return ResultDto.success(challengeDto);
	}
	
	// 챌린지 개설: 인증 필수
	@PostMapping
	public ResultDto<Long> insertChallenge(
				@Valid @RequestBody ChallengeDto challengeDto,
				@AuthenticationPrincipal Object principal
			) {
		
		// 인증 여부 검증
		if (!(principal instanceof MyUserDetails userDetails)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요한 서비스입니다.");
	    }
		
		Long savedId = challengeService.insertChallenge(challengeDto, userDetails.getId());
		
		return ResultDto.success(savedId);
	}

	// 챌린지 수정: 인증 필수
	@PutMapping
	public ResultDto<Long> updateChallenge(
				@Valid @RequestBody ChallengeDto challengeDto,
				@AuthenticationPrincipal Object principal
			) {

		// 인증 여부 검증
		if (!(principal instanceof MyUserDetails userDetails)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요한 서비스입니다.");
		}
		
		Long savedId = challengeService.updateChallenge(challengeDto, userDetails.getId());
		
		return ResultDto.success(savedId);
	}
	
	// 챌린지 삭제: 인증 필수
	@DeleteMapping("/{id}")
	public ResultDto<Void> deleteChallenge(
				@PathVariable("id") Long challengeId,
				@AuthenticationPrincipal Object principal
			) {
		// 인증 여부 검증
		if (!(principal instanceof MyUserDetails userDetails)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요한 서비스입니다.");
	    }
		
		challengeService.deleteChallenge(challengeId, userDetails.getId());
		
		return ResultDto.success();
	}
	
	
	//////////////  챌린지 참여 - 전부 인증 필수 ////////////////
	
	// data = 참여 챌린지 id
	@PostMapping("/{id}/participations")
	public ResultDto<Long> participate(
				@PathVariable("id") Long challengeId,
				// SecurityContextHolder 에서 MyUserDetails 의 id 만 추출
				// MyUserDetails 의 getter 이름이랑 expression 문자열이 대응해야 함.
				@AuthenticationPrincipal Object principal
			) {
		// 인증 여부 검증
		if (!(principal instanceof MyUserDetails userDetails)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요한 서비스입니다.");
	    }
		
		Long savedId = participationService.participate(challengeId, userDetails.getId());
		
		return ResultDto.success(savedId);
	}
	
	// 특정 챌린지 참여자 목록 조회
	@GetMapping("/{id}/participations")
	public ResultDto<List<ParticipantResponseDto>> listParticipant(
				@PathVariable("id") Long challengeId,
				@AuthenticationPrincipal Object principal
			) {
		// 인증 여부 검증
		if (!(principal instanceof MyUserDetails userDetails)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요한 서비스입니다.");
	    }
		
		List<ParticipantResponseDto> data = participationService.listParticipant(challengeId);
		
		return ResultDto.success(data);
	}
	
	// 특정 사용자의 참여 챌린지 목록 조회
	@GetMapping("/my/participations")
	public ResultDto<List<ParticipantResponseDto>> listMyParticipation(
				@AuthenticationPrincipal Object principal
			) {
		// 인증 여부 검증
		if (!(principal instanceof MyUserDetails userDetails)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요한 서비스입니다.");
	    }
		
		List<ParticipantResponseDto> data = participationService.listParticipant(userDetails.getId());
		
		return ResultDto.success(data);
	}
}
