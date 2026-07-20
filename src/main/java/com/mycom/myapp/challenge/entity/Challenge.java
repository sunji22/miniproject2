package com.mycom.myapp.challenge.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mycom.myapp.challenge.domain.ChallengeStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Challenge {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "challenge_id")
	private Long id;
	
//	@ManyToOne(fetch = FetchType.LAZY)
//	private User host;
	
	private String title;
	private String description;
	private int depositAmount;
	private int requiredCount;
	private LocalDate startDate; 
	private LocalDate endDate; 
	
	@Enumerated(EnumType.STRING)
	private ChallengeStatus status;
	
	private LocalDateTime createdAt;
}
