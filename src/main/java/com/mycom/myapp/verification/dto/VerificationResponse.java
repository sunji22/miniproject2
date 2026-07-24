package com.mycom.myapp.verification.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mycom.myapp.verification.entity.Verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 인증글 "응답" DTO (서버 -> 클라이언트)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationResponse {

    private Long id;
    private Long challengeId;
    private Long participationId;
    private Long userId;
    private String userName;
    private String imageUrl;
    private String content;
    private LocalDate verifiedDate;
    private long checkCount;        // 지금까지 받은 상호체크 수
    private long requiredChecks;    // 성공 판정에 필요한 체크 수 (= 참여자수 - 1)
    private boolean succeeded;      // 정원을 채워 성공 판정이 끝났는지
    private boolean mine;           // 조회한 사람이 작성자인지
    private boolean checkedByMe;    // 조회한 사람이 이미 체크했는지
    private LocalDateTime createdAt;

    // 엔티티 -> 응답 DTO
    public static VerificationResponse fromEntity(Verification verification,
                                                  long checkCount,
                                                  long requiredChecks,
                                                  Long viewerUserId,
                                                  boolean checkedByMe) {
        Long writerId = verification.getUser().getUserId();

        return VerificationResponse.builder()
                .id(verification.getId())
                .challengeId(verification.getParticipation().getChallenge().getId())
                .participationId(verification.getParticipation().getId())
                .userId(writerId)
                .userName(verification.getUser().getName())
                .imageUrl(verification.getImageUrl())
                .content(verification.getContent())
                .verifiedDate(verification.getVerifiedDate())
                .checkCount(checkCount)
                .requiredChecks(requiredChecks)
                .succeeded(verification.isSucceeded())
                .mine(writerId.equals(viewerUserId))
                .checkedByMe(checkedByMe)
                .createdAt(verification.getCreatedAt())
                .build();
    }
}
