package com.mycom.myapp.verification.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mycom.myapp.common.ResultDto;
import com.mycom.myapp.common.exception.MissingVerificationFilterException;
import com.mycom.myapp.config.MyUserDetails;
import com.mycom.myapp.verification.dto.VerificationCreateRequest;
import com.mycom.myapp.verification.dto.VerificationResponse;
import com.mycom.myapp.verification.dto.VerificationUpdateRequest;
import com.mycom.myapp.verification.service.VerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 인증글 API
@RestController
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    // 인증글 등록 - 작성자는 body 가 아니라 토큰에서 꺼낸다
    @PostMapping("/api/verifications")
    public ResultDto<VerificationResponse> createVerification(
            @Valid @RequestBody VerificationCreateRequest req,
            @AuthenticationPrincipal MyUserDetails me) {
        VerificationResponse verificationResponse = verificationService.createVerification(req, me.getId());
        return ResultDto.success(verificationResponse);
    }

    // 인증글 수정 - 작성자 본인 + 아직 상호체크를 안 받은 글만
    @PutMapping("/api/verifications/{id}")
    public ResultDto<VerificationResponse> updateVerification(
            @PathVariable("id") Long id,
            @Valid @RequestBody VerificationUpdateRequest req,
            @AuthenticationPrincipal MyUserDetails me) {
        VerificationResponse verificationResponse = verificationService.updateVerification(id, req, me.getId());
        return ResultDto.success(verificationResponse);
    }

    // 인증글 단건 조회
    @GetMapping("/api/verifications/{id}")
    public ResultDto<VerificationResponse> detailVerification(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal MyUserDetails me) {
        VerificationResponse verificationResponse = verificationService.detailVerification(id, me.getId());
        return ResultDto.success(verificationResponse);
    }

    /*
     * 인증글 목록
     *   ?challengeId=6                    -> 챌린지 전체 기간 피드
     *   ?challengeId=6&date=2026-07-23    -> 그날 올라온 인증글 (체크 누르는 화면)
     *   ?participationId=9                -> 특정 참여자의 인증 이력
     *   ?participationId=9&date=...       -> 그 참여자의 그날 인증글
     */
    @GetMapping("/api/verifications")
    public ResultDto<List<VerificationResponse>> listVerification(
            @RequestParam(value = "challengeId", required = false) Long challengeId,
            @RequestParam(value = "participationId", required = false) Long participationId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal MyUserDetails me) {

        if (challengeId == null && participationId == null) {
            throw new MissingVerificationFilterException();
        }

        List<VerificationResponse> data = (challengeId != null)
                ? verificationService.listByChallenge(challengeId, date, me.getId())
                : verificationService.listByParticipation(participationId, date, me.getId());

        return ResultDto.success(data);
    }

    // 상호체크
    @PostMapping("/api/verifications/{id}/check")
    public ResultDto<Void> checkVerification(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal MyUserDetails me) {
        verificationService.checkVerification(id, me.getId());
        return ResultDto.success();
    }
}
