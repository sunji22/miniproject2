package com.mycom.myapp.challenge.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
	
	// RECRUITING -> ONGOING 상태 변경 메소드
	// 시작일 자정(00시)에 바로 시작
	@Modifying
    @Query("UPDATE Challenge c SET c.status = 'ONGOING' WHERE c.startDate <= :now AND c.status = 'RECRUITING'")
    int updateStatusToOngoing(@Param("now") LocalDate now);

	// ONGOING -> CLOSED 상태 변경 메소드
	// 종료일의 마지막 24시에 종료
	// 예: 2026-07-23 종료일 일 떄, 자정(00시)에 2026-07-24 가 now 로 들어오면 상태 변경
    @Modifying
    @Query("UPDATE Challenge c SET c.status = 'CLOSED' WHERE c.endDate < :now AND c.status = 'ONGOING'")
    int updateStatusToClosed(@Param("now") LocalDate now);
}
