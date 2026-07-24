package com.mycom.myapp.verification.service;

import java.time.LocalDate;
import java.util.List;

import com.mycom.myapp.verification.dto.VerificationCreateRequest;
import com.mycom.myapp.verification.dto.VerificationResponse;
import com.mycom.myapp.verification.dto.VerificationUpdateRequest;

public interface VerificationService {

    // 인증글 등록 (본인 참여 챌린지 / 기간 중 / 하루 1회)
    VerificationResponse createVerification(VerificationCreateRequest req, Long userId);

    // 인증글 수정 (작성자 본인 / 아직 상호체크를 안 받은 글만)
    VerificationResponse updateVerification(Long verificationId, VerificationUpdateRequest req, Long userId);

    // 인증글 단건 조회
    VerificationResponse detailVerification(Long verificationId, Long viewerUserId);

    // 챌린지 피드 - verifiedDate 가 null 이면 전체 기간
    List<VerificationResponse> listByChallenge(Long challengeId, LocalDate verifiedDate, Long viewerUserId);

    // 특정 참여자 이력 - verifiedDate 가 null 이면 전체 기간
    List<VerificationResponse> listByParticipation(Long participationId, LocalDate verifiedDate, Long viewerUserId);

    // 상호체크 (같은 챌린지 참여자가 남의 인증글을 확인)
    void checkVerification(Long verificationId, Long checkerUserId);
}
