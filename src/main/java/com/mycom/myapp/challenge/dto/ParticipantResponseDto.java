package com.mycom.myapp.challenge.dto;

import java.time.LocalDateTime;

import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.entity.ParticipationStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 특정 챌린지의 참여자 정보 응답 DTO
 * List<Participation> -> List<ChallengeParticipantResponseDto>
 */
@Getter
@Builder
public class ParticipantResponseDto {

	private Long participationId;   // 참여 식별자 (PK)
	
    private Long userId;            // 참여자 유저 ID
    // !! join fetch 필수
    private String userName;        // 참여자 이름
    
    private int successCount;       // 해당 챌린지 성공 횟수
    private ParticipationStatus status;   // 참여 상태 (JOINED/SUCCESS/FAILED)
    private LocalDateTime joinedAt; // 참여 일시 (DB의 created_at)

    // 참여 엔티티에서 User 정보만 추출 -> DTO로 생성
    public static ParticipantResponseDto from(Participation participation) {
        return ParticipantResponseDto.builder()
                .participationId(participation.getId())
                .userId(participation.getUser().getUserId())
                .userName(participation.getUser().getName())
                .successCount(participation.getSuccessCount())
                .status(participation.getStatus())
                .joinedAt(participation.getCreatedAt())
                .build();
    }
}
