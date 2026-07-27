package com.mycom.myapp.point.domain;

import java.util.List;

import com.mycom.myapp.challenge.entity.Participation;

import lombok.Builder;
import lombok.Getter;

// 챌린지 하나의 정산 계산 결과 (돈은 아직 안 옮긴 상태)
// 실제 정산(settleChallenge)과 미리보기(previewSettlement)가 이 결과를 공유
@Getter
@Builder
public class SettlementCalculation {

	private final int successCount;			// 성공자 수
	private final int failCount;			// 실패자 수
	private final int totalPenaltyAmount;	// 실패자 보증금 합계 (몰수 총액)
	private final int rewardPerPerson;		// 성공자 1명당 분배액 (성공자가 없으면 0)
	private final List<Line> lines;			// 참여자별 내역

	// 참여자 한 명의 정산 내역
	@Getter
	@Builder
	public static class Line {

		// 상태 변경(SUCCESS/FAILED)과 환불/몰수 호출에 필요해 엔티티를 그대로 들고 있는다
		private final Participation participation;

		private final boolean success;	// 성공 판정 여부
		private final int refund;		// 성공: 보증금 반환액, 실패: 0
		private final int reward;		// 성공: 몰수분 분배액, 실패: 0
		private final int penalty;		// 실패: 몰수액, 성공: 0
	}
}
