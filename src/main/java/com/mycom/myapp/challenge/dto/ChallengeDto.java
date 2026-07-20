package com.mycom.myapp.challenge.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.entity.Challenge;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DB -> 엔티티 -> 응답용 조회 결과 DTO
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDto {

	private Long id;			// 등록 시에는 사용 x (Auto Increment)
	
//	private Long hostId;		// User 엔티티 추가 필요
	
	private String title;
	private String description; // 상세 조회 시에만 사용
	private int depositAmount;
	private int requiredCount; 
	private LocalDate startDate; 
	private LocalDate endDate; 
	private ChallengeStatus status;	 // 등록 시에는 사용 x (Default: 모집중)
	
	// (일단은)직접 주입 x. DB에서 DEFAULT_GENERATED
	// -> 후에 리팩토링 필요. save 전에 값이 필요한 비즈니스 로직이 필요할 수 있음.
	private LocalDateTime createdAt; 
	
	// 🎯 Entity -> DTO 변환: 정적 팩토리 메서드 (from)
	public static ChallengeDto from(Challenge challenge) {
		return ChallengeDto.builder().id(challenge.getId())
									  .title(challenge.getTitle())
									  .description(challenge.getDescription())
									  .depositAmount(challenge.getDepositAmount())
									  .requiredCount(challenge.getRequiredCount())
									  .startDate(challenge.getStartDate())
									  .endDate(challenge.getEndDate())
									  .status(challenge.getStatus())
									  .createdAt(challenge.getCreatedAt())
									  .build();
	}
	
	// 🎯 DTO -> Entity 변환: 인스턴스 메서드 (toEntity)
    public Challenge toEntity() {
        return Challenge.builder().title(this.title)
					                .description(this.getDescription())
					                .depositAmount(this.depositAmount)
									.depositAmount(this.getDepositAmount())
									.requiredCount(this.getRequiredCount())
									.startDate(this.getStartDate())
									.endDate(this.getEndDate())
					                .build();
    }
}
