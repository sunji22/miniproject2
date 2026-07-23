package com.mycom.myapp.challenge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mycom.myapp.challenge.entity.Participation;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    Optional<Participation> findByChallenge_IdAndUser_UserId(Long challengeId, Long userId);
    
    @Query("SELECT p FROM Participation p "
    		+ "JOIN FETCH p.challenge "
    		+ "WHERE p.user.userId = :userId")
    List<Participation> findByUser_UserId(@Param("userId") Long userId);
    
    @Query("SELECT p FROM Participation p "
    		+ "JOIN FETCH p.user "
    		+ "WHERE p.challenge.id = :challengeId")
    List<Participation> findByChallenge_Id(@Param("challengeId") Long challengeId);

    boolean existsByChallenge_IdAndUser_UserId(Long challengeId, Long userId);
}
