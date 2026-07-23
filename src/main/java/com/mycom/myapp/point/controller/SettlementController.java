package com.mycom.myapp.point.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycom.myapp.common.ResultDto;
import com.mycom.myapp.common.exception.UserNotFoundException;
import com.mycom.myapp.config.MyUserDetails;
import com.mycom.myapp.point.dto.SettlementRequestDto;
import com.mycom.myapp.point.dto.SettlementResultResponseDto;
import com.mycom.myapp.point.service.SettlementService;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {
	
	private final UserRepository userRepository;
	private final SettlementService settlementService;
	
	// SecurityContextHolder 에서 현재 로그인한 userId 추출
	private Long getCurrentUserId(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        MyUserDetails userDetails = (MyUserDetails) auth.getPrincipal();
        return userDetails.getId();
    }
	
	// 전원 실패 시 몰수
	@PostMapping("/penalty-all")
	public ResultDto<Void> penaltyAll(@RequestBody SettlementRequestDto dto){
		settlementService.penaltyAll(dto.getChallengeId());
		return ResultDto.success();
	}
	
	// 환불
	@PostMapping("/refund")
	public ResultDto<Void> refund(
			HttpServletRequest request,
			@RequestBody SettlementRequestDto dto){
		Long userId = getCurrentUserId(request);
		settlementService.refund(userId, dto.getParticipationId(), dto.getAmount());
		return ResultDto.success();
	}
	
	// 몰수 (실패자만)
	@PostMapping("/penalty")
	public ResultDto<Void> penalty(
			HttpServletRequest request,
			@RequestBody SettlementRequestDto dto){
		Long userId = getCurrentUserId(request);
		settlementService.penalty(userId, dto.getParticipationId(), dto.getAmount());
		return ResultDto.success();
	}
	
	// 분배
	@PostMapping("/reward")
	public ResultDto<Void> reward(
			HttpServletRequest request,
			@RequestBody SettlementRequestDto dto){
		Long userId = getCurrentUserId(request);
		settlementService.reward(userId, dto.getParticipationId(), 
				dto.getTotalPenaltyAmount(), dto.getSuccessCount());
		return ResultDto.success();
	}
	
	// 정산 실행
	@PostMapping("/settle/{challengeId}")
	public ResultDto<Void> settle(
			HttpServletRequest request,
			@PathVariable("challengeId") Long challengeId){
		Long userId = getCurrentUserId(request);
		settlementService.settleChallenge(challengeId, userId);
		return ResultDto.success();
	}
	
	// 정산 결과 조회
	@GetMapping("/result/{challengeId}")
	public ResultDto<SettlementResultResponseDto> getResult(
			HttpServletRequest request,
			@PathVariable("challengeId") Long challengeId){
		Long userId = getCurrentUserId(request);
		
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
