package com.mycom.myapp.point.service;

import com.mycom.myapp.point.dto.SettlementPreviewResponseDto;

public interface SettlementService {

	void penaltyAll(Long challengeId);

	void refund(Long userId, Long participationId, int amount);

	void penalty(Long userId, Long participationId, int amount);

	void reward(Long userId, Long participationId, int totalPenaltyAmount, int successCount);
	
	void settleChallenge(Long challengeId, Long hostId);		// 정산 실행 메서드 추가
	
	int getSettlementAmount(Long challengeId);					// 정산 결과 조회 메서드 추가
	
	SettlementPreviewResponseDto previewSettlement(Long challengeId, Long hostId);		// 정산 미리보기 메서드 추가
}
