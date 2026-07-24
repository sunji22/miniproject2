package com.mycom.myapp.point.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SettlementPreviewResponseDto {
	private Long challengeId;							// 챌린지 id
	private String challengeTitle;						// 챌린지 제목
	private int depositAmount;							// 보증금 금액
	private int requiredCount;							// 성공 조건 횟수
	private int totalParticipants;						// 총 챌린지 참여자 수
	private int successCount;							// 성공자 수
	private int failCount;								// 실패자 수
	private int totalPenaltyAmount;						// 실패자 보증금 합계
	private int rewardPerPerson;						// 성공자 1명당 보상
	private List<ParticipantPreview> participants;		// 예상 정산 리스트
	
	@Builder
	@Data
	public static class ParticipantPreview{
		private Long userId;
		private String userName;
		private int currentSuccessCount;				// 현재 인증 횟수
		private boolean success;						// 예상 성공 여부
		private int refundAmount;						// 예상 환불 금액
		private int rewardAmount;						// 예상 보상 금액
		private int penaltyAmount;						// 예상 몰수 금액
		private int currentBalance;						// 현재 보유 잔액
		private int expectedBalance;					// 정산 후 예상 잔액
	}
}
