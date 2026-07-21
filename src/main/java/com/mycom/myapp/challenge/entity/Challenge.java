package com.mycom.myapp.challenge.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Challenge {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "challenge_id")
	private Long id;
	
	@ManyToOne(fetch = FetchType.EAGER)
	private User host;
	
	private String title;
	private String description;
	private int depositAmount;
	private int requiredCount;
	private LocalDate startDate; 
	private LocalDate endDate; 
	
	@Builder.Default // 🎯 빌더 패턴을 통해 객체를 주조할 때도 아래 기본값을 강제 lock-in
	@Enumerated(EnumType.STRING)
	private ChallengeStatus status = ChallengeStatus.RECRUITING;
	
//	@CreatedDate
	@Column(updatable = false, nullable = false) // 생성일 수정 불가
	private LocalDateTime createdAt;
}
