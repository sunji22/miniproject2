package com.mycom.myapp.challenge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.entity.ParticipationStatus;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    Optional<Participation> findByChallenge_IdAndUser_UserId(Long challengeId, Long userId);
    
    @Query("SELECT p FROM Participation p "
    		+ "JOIN FETCH p.challenge "
    		+ "WHERE p.user.userId = :userId")
    List<Participation> findByUser_UserId(@Param("userId") Long userId);
    
    @Query("SELECT p FROM Participation p "
    		+ "JOIN FETCH p.user "
    		+ "WHERE p.challenge.id = :challengeId AND p.status = :status")
    List<Participation> findByChallenge_IdAndStatus(
    	@Param("challengeId") Long challengeId,
    	@Param("status") ParticipationStatus status
	);

    boolean existsByChallenge_IdAndUser_UserId(Long challengeId, Long userId);

    // JOINED 상태인 참여만 단건 조회
    Optional<Participation> findByChallenge_IdAndUser_UserIdAndStatus(Long challengeId, Long userId, ParticipationStatus joined);
	
    // 챌린지 참여자 수 (상호체크에 필요한 체크 수 = 참여자수 - 1 계산에 사용)
    long countByChallenge_Id(Long challengeId);
}
