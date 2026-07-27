package com.mycom.myapp.point.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycom.myapp.common.ResultDto;
import com.mycom.myapp.common.exception.UserNotFoundException;
import com.mycom.myapp.point.dto.SettlementPreviewResponseDto;
import com.mycom.myapp.point.dto.SettlementRequestDto;
import com.mycom.myapp.point.dto.SettlementResultResponseDto;
import com.mycom.myapp.point.service.SettlementService;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {
	
	private final UserRepository userRepository;
	private final SettlementService settlementService;
	
	// 전원 실패 시 몰수
	@PostMapping("/penalty-all")
	public ResultDto<Void> penaltyAll(@RequestBody SettlementRequestDto dto){
		settlementService.penaltyAll(dto.getChallengeId());
		return ResultDto.success();
	}
	
	// 환불
	@PostMapping("/refund")
	public ResultDto<Void> refund(
			@AuthenticationPrincipal(expression = "id") Long userId,
			@RequestBody SettlementRequestDto dto){
		settlementService.refund(userId, dto.getParticipationId(), dto.getAmount());
		return ResultDto.success();
	}
	
	// 몰수 (실패자만)
	@PostMapping("/penalty")
	public ResultDto<Void> penalty(
			@AuthenticationPrincipal(expression = "id") Long userId,
			@RequestBody SettlementRequestDto dto){
		settlementService.penalty(userId, dto.getParticipationId(), dto.getAmount());
		return ResultDto.success();
	}
	
	// 분배
	@PostMapping("/reward")
	public ResultDto<Void> reward(
			@AuthenticationPrincipal(expression = "id") Long userId,
			@RequestBody SettlementRequestDto dto){
		settlementService.reward(userId, dto.getParticipationId(), 
				dto.getTotalPenaltyAmount(), dto.getSuccessCount());
		return ResultDto.success();
	}
	
	// 예상 정산 미리보기
	// 조회는 참여자 전원 가능, 실행([정산하기])은 호스트만
	@GetMapping("/preview/{challengeId}")
	public ResultDto<SettlementPreviewResponseDto> preview(
			@AuthenticationPrincipal(expression = "id") Long userId,
			@PathVariable("challengeId") Long challengeId){
		return ResultDto.success(settlementService.previewSettlement(challengeId, userId));
	}

	// 정산 실행
	@PostMapping("/settle/{challengeId}")
	public ResultDto<Void> settle(
			@AuthenticationPrincipal(expression = "id") Long userId,
			@PathVariable("challengeId") Long challengeId){
		settlementService.settleChallenge(challengeId, userId);
		return ResultDto.success();
	}
	
	// 정산 결과 조회
	@GetMapping("/result/{challengeId}")
	public ResultDto<SettlementResultResponseDto> getResult(
			@AuthenticationPrincipal(expression = "id") Long userId,
			@PathVariable("challengeId") Long challengeId){
		
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		
		int settlementAmount = settlementService.getSettlementAmount(challengeId);
		
		SettlementResultResponseDto response = SettlementResultResponseDto.builder()
				.userId(userId)
				.challengeId(challengeId)
				.amount(settlementAmount)
				.balanceAfter(user.getPointBalance())
				.build();
		return ResultDto.success(response);
	}
}
