package com.mycom.myapp.challenge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mycom.myapp.challenge.entity.Challenge;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

	// status 필터링 -> 생성일 순 정렬
	Page<Challenge> findByStatusOrderByCreatedAt(String status, Pageable pageable);
	
	// Title 필터링 -> 생성일 순 정렬
	List<Challenge> findByTitleOrderByCreatedAt(String status, Pageable pageable);
}
