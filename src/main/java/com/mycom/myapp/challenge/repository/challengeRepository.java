package com.mycom.myapp.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mycom.myapp.challenge.entity.Challenge;

public interface challengeRepository extends JpaRepository<Challenge, Long> {

}
