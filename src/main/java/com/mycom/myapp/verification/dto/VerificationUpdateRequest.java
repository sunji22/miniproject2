package com.mycom.myapp.verification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 인증글 수정 "요청" DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationUpdateRequest {

    @NotBlank(message = "인증 사진 URL 은 필수입니다.")
    private String imageUrl;

    private String content;
}
