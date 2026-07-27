package com.mycom.myapp.point.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycom.myapp.common.ResultDto;
import com.mycom.myapp.point.dto.PointHistoryResponseDto;
import com.mycom.myapp.point.dto.PointRequestDto;
import com.mycom.myapp.point.entity.PointHistory;
import com.mycom.myapp.point.repository.PointHistoryRepository;
import com.mycom.myapp.point.service.PointService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Slf4j
public class PointController {
	private final PointService pointService;
	private final PointHistoryRepository pointHistoryRepository;

	// #1. 포인트 충전
	@PostMapping("/charge")
	public ResultDto<Void> charge(
			@AuthenticationPrincipal(expression = "id") Long userId,
			@RequestBody PointRequestDto dto){
		pointService.chargePoint(userId, dto.getAmount());
		return ResultDto.success();
	}
	
	// #2. 포인트 조회
	@GetMapping("/balance")
	public ResultDto<PointHistoryResponseDto> getBalance(@AuthenticationPrincipal(expression = "id") Long userId){
		int balance = pointService.getPointBalance(userId);
		PointHistoryResponseDto response = PointHistoryResponseDto.builder()
				.balance(balance)
				.build();
		return ResultDto.success(response);
	}
	
	// #3. 포인트 이력 조회
	@GetMapping("/history")
	public ResultDto<List<PointHistoryResponseDto>> getHistory(@AuthenticationPrincipal(expression = "id") Long userId){
		List<PointHistory> historyList = pointHistoryRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
		List<PointHistoryResponseDto> response = historyList.stream()
												.map(PointHistoryResponseDto::from)
												.collect(Collectors.toList());
		return ResultDto.success(response);
	}
	
	// #4. 포인트 차감
	@PostMapping("/withdraw")
	public ResultDto<Void> withdraw(
			@AuthenticationPrincipal(expression = "id") Long userId,
			@RequestBody PointRequestDto dto){
		pointService.withdrawPoint(userId, dto.getAmount());
		return ResultDto.success();
	}
}
