package com.mycom.myapp.challenge.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycom.myapp.challenge.service.ParticipationService;
import com.mycom.myapp.common.ResultDto;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "참여 API", description = "참여 삭제")
@RestController
@RequestMapping("/api/participations")
@RequiredArgsConstructor
public class ParticipationController {
	
	private final ParticipationService participationService;
	
	@DeleteMapping("/{id}")
	public ResultDto<Integer> deleteParticipation(
				@PathVariable("id") Long participationId,
				@AuthenticationPrincipal(expression = "id") Long userId
			) {
		
		int returnedDeposit = participationService.deleteParticipation(participationId, userId);
		
		return ResultDto.success(returnedDeposit);
	}
}
