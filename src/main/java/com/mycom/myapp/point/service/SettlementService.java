package com.mycom.myapp.point.service;

public interface SettlementService {

	void penaltyAll(Long challengeId);

	void refund(Long userId, Long participationId, int amount);

	void penalty(Long userId, Long participationId, int amount);

	void reward(Long userId, Long participationId, int totalPenaltyAmount, int successCount);
	
	void settleChallenge(Long challengeId, Long hostId);
}
