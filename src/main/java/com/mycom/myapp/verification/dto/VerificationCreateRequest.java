package com.mycom.myapp.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 인증글 등록 "요청" DTO (클라이언트 -> 서버)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationCreateRequest {

    @NotNull(message = "챌린지 id 는 필수입니다.")
    private Long challengeId;

    // 업로드 인프라가 없어 URL 문자열을 필수로
    @NotBlank(message = "인증 사진 URL 은 필수입니다.")
    private String imageUrl;

    private String content;
}
