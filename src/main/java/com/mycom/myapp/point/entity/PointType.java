package com.mycom.myapp.point.entity;

public enum PointType {
	CHARGE,					// 포인트 충전
	DEPOSIT_LOCK,			// 챌린지 참여 -> 보증금 잠금
	DEPOSIT_REFUND,			// 챌린지 성공 -> 보증금 환불
	PENALTY,				// 챌린지 실패 -> 보증금 몰수
	REWARD,					// 몰수분 분배
	WITHDRAW				// 포인트 차감
}
