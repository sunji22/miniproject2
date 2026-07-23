package com.mycom.myapp.challenge.dto;

import java.time.LocalDateTime;

import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.entity.ParticipationStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 나의 참여 챌린지 정보 응답 DTO
 * List<Participation> -> List<MyParticipationResponseDto>
 */
@Getter
@Builder
public class MyParticipationResponseDto {

	private Long participationId;    // 참여 식별자 (PK)
	
	
    private Long challengeId;        // 챌린지 ID
    // !! join fetch 필수
    private String challengeTitle;   // 챌린지 제목
    private String challengeStatus;  // 챌린지 자체 상태 (RECRUITING/ONGOING/CLOSED)
    
    private int successCount;        // 내 성공 횟수
    private ParticipationStatus myStatus;   // 내 참여 상태 (JOINED/SUCCESS/FAILED)
    private LocalDateTime joinedAt;  // 내가 참여한 일시 (DB의 created_at)

    public static MyParticipationResponseDto from(Participation participation) {
        return MyParticipationResponseDto.builder()
                .participationId(participation.getId())
                .challengeId(participation.getChallenge().getId())
                .challengeTitle(participation.getChallenge().getTitle())
                .challengeStatus(participation.getChallenge().getStatus().name())
                .successCount(participation.getSuccessCount())
                .myStatus(participation.getStatus())
                .joinedAt(participation.getCreatedAt())
                .build();
    }
}
