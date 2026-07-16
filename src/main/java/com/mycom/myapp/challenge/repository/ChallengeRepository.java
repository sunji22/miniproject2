package com.mycom.myapp.challenge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mycom.myapp.challenge.entity.Challenge;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

	List<Challenge> findByStatus(String status);
}
