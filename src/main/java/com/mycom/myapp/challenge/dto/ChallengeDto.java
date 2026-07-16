package com.mycom.myapp.challenge.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDto {

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
