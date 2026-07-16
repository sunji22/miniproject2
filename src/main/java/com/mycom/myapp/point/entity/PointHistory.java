package com.mycom.myapp.point.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data		// getter, setter, 생성자 등 생성
@NoArgsConstructor		// 기본 생성자 필수
@AllArgsConstructor		// 모든 필드를 포함한 생성자 생성
@Entity
@Table(name="point_history")
public class PointHistory {

	// 기본 키 설정 (BIGINT -> Long 타입으로 매핑)
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="point_history_id")
	private Long pointHistoryid;
	
	@ManyToOne(fetch=FetchType.LAZY)		// 포인트 이력 조회 많을 수 있기 때문에 지연 로딩 처리
	@JoinColumn(name="user_id", nullable=false)
	private User user;
	
	@ManyToOne(fetch=FetchType.LAZY)		// 포인트 이력 조회 많을 수 있기 때문에 지연 로딩 처리
	@JoinColumn(name="participation_id")	// 정산 or 충전은 참여 내역에 없을 수도 있어서 nullable=true
	private Participation participation;
	
	@Column(nullable=false)
	private int amout;
	
	@Enumerated(EnumType.STRING)			// DB -> "CHARGE/DEPOSIT_LOCK/DEPOSIT_REFUND/PENALTY/REWARD" 문자열 저장
	@Column(nullable=false)
	private PointType type;
	
	@Column(name="balance_after", nullable=false)
	private int balaceAfter;
	
	@Column(name="created_at", nullable=false, updatable=false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
