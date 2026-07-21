package com.mycom.myapp.challenge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mycom.myapp.challenge.entity.Participation;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    Optional<Participation> findByChallenge_IdAndUser_Id(Long challengeId, Long userId);

    List<Participation> findByUser_Id(Long userId);
    
    List<Participation> findByChallenge_Id(Long challengeId);

    boolean existsByChallenge_IdAndUser_Id(Long challengeId, Long userId);
}
