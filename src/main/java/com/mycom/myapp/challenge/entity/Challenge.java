package com.mycom.myapp.challenge.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
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
	private Long id;
	
//	@ManyToOne(fetch = FetchType.LAZY)
//	private User host;
	
	private String title;
	private String description;
	private int deposit_amount;
	private int required_count; 
	private LocalDate start_date; 
	private LocalDate end_date; 
	private String status;
	private LocalDateTime created_at;
}
