package com.mycom.myapp.challenge.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.domain.SettlementStatus;
import com.mycom.myapp.challenge.entity.Challenge;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

	// status 필터링 -> 생성일 순 정렬
	Page<Challenge> findByStatusOrderByCreatedAt(ChallengeStatus status, Pageable pageable);
	
	// Title 필터링 -> 생성일 순 정렬
	List<Challenge> findByTitleOrderByCreatedAt(ChallengeStatus status, Pageable pageable);
	
	// 정산 대상 챌린지 조회 ( 종료인 지남 + 진행 중 + 정산 대기)
	List<Challenge> findByEndDateBeforeAndStatusAndSettlementStatus(
			LocalDate date, 
			ChallengeStatus status, 
			SettlementStatus settlementStatus
	);
	
}
