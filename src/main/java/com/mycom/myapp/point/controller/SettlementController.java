package com.mycom.myapp.point.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycom.myapp.common.ResultDto;
import com.mycom.myapp.config.MyUserDetails;
import com.mycom.myapp.point.dto.SettlementRequestDto;
import com.mycom.myapp.point.service.SettlementService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {
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
}
