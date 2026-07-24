package com.mycom.myapp.challenge.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.user.entity.User;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
	
	private Long hostId;		// 요청 시에는 사용 x
	
	@NotBlank(message = "제목은 필수입니다.")
	private String title;
	private String description; // 상세 조회 시에만 사용
	private int depositAmount;
	
	@Min(value = 1, message = "최소 인정 횟수는 1회 이상이어야 합니다.")
	private int requiredCount; 
	
	@NotNull(message = "시작일은 필수입니다.")
	private LocalDate startDate; 
	@NotNull(message = "종료일은 필수입니다.")
	private LocalDate endDate; 
	private ChallengeStatus status;	 // 등록 시에는 사용 x (Default: 모집중)
	
	// (일단은) Service 에서 현재시각 주입 -> 리팩토링 필요할듯
	private LocalDateTime createdAt;
	
	// 추가: 상세페이지 응답 시 사용자의 참여 id 추가
	// 상세페이지에서 삭제 요청 시 필요 & 사용자 참여 여부 판단 가능 (null 이면 참여중x)
	private Long participationId;
	
	// 🎯 Entity -> DTO 변환: 정적 팩토리 메서드 (from)
	public static ChallengeDto from(Challenge challenge) {
		return ChallengeDto.builder().id(challenge.getId())
				// .getId() 추가 쿼리 없음 (N+1 발생 x)
				// User 프록시 객체가 host_id 를 갖고있기 때문
				.hostId(challenge.getHost() != null ? challenge.getHost().getUserId() : null)
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
    public Challenge toEntity(User user) {
        return Challenge.builder().id(this.getId())
        						.host(user)
        						.title(this.title)
				                .description(this.getDescription())
				                .depositAmount(this.depositAmount)
								.depositAmount(this.getDepositAmount())
								.requiredCount(this.getRequiredCount())
								.startDate(this.getStartDate())
								.endDate(this.getEndDate())
								.createdAt(createdAt)
				                .build();
    }
}
