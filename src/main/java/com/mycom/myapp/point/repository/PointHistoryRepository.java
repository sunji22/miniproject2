package com.mycom.myapp.point.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mycom.myapp.point.entity.PointHistory;
import com.mycom.myapp.point.entity.PointType;

// 쿼리 자동 생성을 위해 JpaRepository 상속
@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long>{
	
	// 특정 회원(회원 id 를 통해) 포인트 이력 조회 (최신순)
	List<PointHistory> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
	
	// 챌린지별 정산 금액 합계 조회
	@Query("""
			SELECT SUM(ph.amount) 
			  FROM PointHistory ph 
			 WHERE ph.participation.challenge.id = :challengeId 
			   AND ph.type IN :types
			""")
	Integer sumAmountByParticipation_Challenge_IdAndTypeIn(
            @Param("challengeId") Long challengeId, 
            @Param("types") List<PointType> types
    );
}
