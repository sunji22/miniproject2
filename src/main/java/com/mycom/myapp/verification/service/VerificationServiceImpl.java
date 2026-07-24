package com.mycom.myapp.verification.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.common.exception.AlreadyCheckedException;
import com.mycom.myapp.common.exception.DuplicateVerificationException;
import com.mycom.myapp.common.exception.NotChallengeParticipantException;
import com.mycom.myapp.common.exception.NotVerificationOwnerException;
import com.mycom.myapp.common.exception.SelfCheckNotAllowedException;
import com.mycom.myapp.common.exception.UserNotFoundException;
import com.mycom.myapp.common.exception.VerificationAlreadyCheckedModifyException;
import com.mycom.myapp.common.exception.VerificationNotFoundException;
import com.mycom.myapp.common.exception.VerificationPeriodException;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;
import com.mycom.myapp.verification.dto.VerificationCreateRequest;
import com.mycom.myapp.verification.dto.VerificationResponse;
import com.mycom.myapp.verification.dto.VerificationUpdateRequest;
import com.mycom.myapp.verification.entity.Verification;
import com.mycom.myapp.verification.entity.VerificationCheck;
import com.mycom.myapp.verification.repository.VerificationCheckRepository;
import com.mycom.myapp.verification.repository.VerificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 인증글 + 상호체크
 *
 *   - 본인이 참여한 챌린지에만 인증글을 쓸 수 있다
 *   - 챌린지 진행 기간 중에만 쓸 수 있다
 *   - 하루 1회만 쓸 수 있다 (날짜는 서버 기준 오늘로 고정)
 *   - 아직 아무도 체크하지 않은 인증글만 수정할 수 있다
 *   - 같은 챌린지의 다른 참여자가 전원 체크하면 그날 인증이 성공 처리된다
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    private final VerificationRepository verificationRepository;
    private final VerificationCheckRepository verificationCheckRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;

    // 등록
    @Override
    public VerificationResponse createVerification(VerificationCreateRequest req, Long userId) {

        Long challengeId = req.getChallengeId();

        // 본인 참여 확인
        Participation participation = participationRepository
                .findByChallenge_IdAndUser_UserId(challengeId, userId)
                .orElseThrow(() -> new NotChallengeParticipantException(challengeId));

        // 인증 날짜는 서버 기준 오늘로 고정
        LocalDate today = LocalDate.now();

        // 챌린지 진행 기간 확인
        Challenge challenge = participation.getChallenge();
        if (today.isBefore(challenge.getStartDate()) || today.isAfter(challenge.getEndDate())) {
            throw new VerificationPeriodException(challenge.getStartDate(), challenge.getEndDate());
        }

        // 하루 1회 확인
        if (verificationRepository.existsByParticipation_IdAndVerifiedDate(participation.getId(), today)) {
            throw new DuplicateVerificationException();
        }

        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 필요 체크 수 = 나를 뺀 나머지 참여자 수
        long requiredChecks = requiredChecks(challengeId);

        // 혼자 하는 챌린지(체크해 줄 사람이 없음)는 등록 즉시 성공 처리
        boolean succeededNow = (requiredChecks == 0);

        Verification verification = new Verification();
        verification.setParticipation(participation);
        verification.setUser(writer);
        verification.setImageUrl(req.getImageUrl());
        verification.setContent(req.getContent());
        verification.setVerifiedDate(today);
        verification.setSucceeded(succeededNow);

        Verification saved = verificationRepository.save(verification);

        if (succeededNow) {
            participation.setSuccessCount(participation.getSuccessCount() + 1);
            log.info("1인 챌린지 인증 즉시 성공 : verificationId = {}, successCount = {}",
                    saved.getId(), participation.getSuccessCount());
        }

        log.info("인증글 등록 : verificationId = {}, challengeId = {}, userId = {}, date = {}",
                saved.getId(), challengeId, userId, today);

        return VerificationResponse.fromEntity(saved, 0L, requiredChecks, userId, false);
    }

    // 수정
    @Override
    public VerificationResponse updateVerification(Long verificationId, VerificationUpdateRequest req, Long userId) {

        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new VerificationNotFoundException(verificationId));

        // 작성자 본인
        if (!verification.getUser().getUserId().equals(userId)) {
            throw new NotVerificationOwnerException(verificationId);
        }

        // 아직 아무도 체크하지 않은 글만
        if (verificationCheckRepository.existsByVerification_Id(verificationId)) {
            throw new VerificationAlreadyCheckedModifyException(verificationId);
        }

        verification.setImageUrl(req.getImageUrl());
        verification.setContent(req.getContent());

        log.info("인증글 수정 : verificationId = {}, userId = {}", verificationId, userId);

        long requiredChecks = requiredChecks(verification.getParticipation().getChallenge().getId());
        return VerificationResponse.fromEntity(verification, 0L, requiredChecks, userId, false);
    }


    // 조회
    @Override
    @Transactional(readOnly = true)
    public VerificationResponse detailVerification(Long verificationId, Long viewerUserId) {

        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new VerificationNotFoundException(verificationId));

        long checkCount = verificationCheckRepository.countByVerification_Id(verificationId);
        long requiredChecks = requiredChecks(verification.getParticipation().getChallenge().getId());
        boolean checkedByMe = verificationCheckRepository
                .existsByVerification_IdAndChecker_UserId(verificationId, viewerUserId);

        return VerificationResponse.fromEntity(verification, checkCount, requiredChecks, viewerUserId, checkedByMe);
    }

    // 챌린지 피드(verifiedDate 를 주면 그날 것만 / 오늘 올라온 인증글을 모아 보고 체크를 누르는 화면용)
    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> listByChallenge(Long challengeId, LocalDate verifiedDate, Long viewerUserId) {

        List<Verification> verifications = (verifiedDate == null)
                ? verificationRepository.findFeedByChallenge(challengeId)
                : verificationRepository.findFeedByChallengeAndDate(challengeId, verifiedDate);

        return toResponses(verifications, requiredChecks(challengeId), viewerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> listByParticipation(Long participationId, LocalDate verifiedDate, Long viewerUserId) {

        List<Verification> verifications = (verifiedDate == null)
                ? verificationRepository.findFeedByParticipation(participationId)
                : verificationRepository.findFeedByParticipationAndDate(participationId, verifiedDate);

        if (verifications.isEmpty()) {
            return new ArrayList<>();
        }

        Long challengeId = verifications.getFirst().getParticipation().getChallenge().getId();

        return toResponses(verifications, requiredChecks(challengeId), viewerUserId);
    }

    // 상호체크
    @Override
    public void checkVerification(Long verificationId, Long checkerUserId) {

        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new VerificationNotFoundException(verificationId));

        Participation participation = verification.getParticipation();
        Long challengeId = participation.getChallenge().getId();

        // 체커가 같은 챌린지 참여자인지
        if (!participationRepository.existsByChallenge_IdAndUser_UserId(challengeId, checkerUserId)) {
            throw new NotChallengeParticipantException(challengeId);
        }

        // 본인 글 자가 체크 차단
        if (verification.getUser().getUserId().equals(checkerUserId)) {
            throw new SelfCheckNotAllowedException();
        }

        // 1인 1체크
        if (verificationCheckRepository.existsByVerification_IdAndChecker_UserId(verificationId, checkerUserId)) {
            throw new AlreadyCheckedException();
        }

        User checker = userRepository.findById(checkerUserId)
                .orElseThrow(() -> new UserNotFoundException(checkerUserId));

        VerificationCheck check = new VerificationCheck();
        check.setVerification(verification);
        check.setChecker(checker);
        verificationCheckRepository.save(check);

        // 정원을 채웠고 아직 반영 전이면 딱 한 번만 successCount 를 올린다
        long checkCount = verificationCheckRepository.countByVerification_Id(verificationId);
        long requiredChecks = requiredChecks(challengeId);

        if (!verification.isSucceeded() && checkCount >= requiredChecks) {
            verification.setSucceeded(true);   // 더티체킹으로 반영
            participation.setSuccessCount(participation.getSuccessCount() + 1);
            log.info("인증글 성공 판정 : verificationId = {}, checkCount = {}/{}, successCount = {}",
                    verificationId, checkCount, requiredChecks, participation.getSuccessCount());
        }
    }

    // 성공 판정에 필요한 상호체크 수 = 참여자 수 - 1 (작성자 본인 제외)
    private long requiredChecks(Long challengeId) {
        return Math.max(participationRepository.countByChallenge_Id(challengeId) - 1, 0);
    }

    // 체크 수와 "내가 체크했는지"를 인증글마다 따로 조회하면 N+1 이므로 id 목록을 한 번에 넘겨 2번의 쿼리로 끝냄
    private List<VerificationResponse> toResponses(List<Verification> verifications,
                                                   long requiredChecks,
                                                   Long viewerUserId) {
        if (verifications.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> ids = new ArrayList<>();
        verifications.forEach(v -> ids.add(v.getId()));

        // [인증글id -> 체크수]
        Map<Long, Long> checkCountMap = new HashMap<>();
        verificationCheckRepository.countGroupedByVerificationIds(ids)
                .forEach(row -> checkCountMap.put((Long) row[0], (Long) row[1]));

        // 내가 이미 체크한 인증글 id 집합
        Set<Long> checkedByMe = new HashSet<>(
                verificationCheckRepository.findCheckedVerificationIds(viewerUserId, ids));

        List<VerificationResponse> responses = new ArrayList<>();
        verifications.forEach(v -> responses.add(VerificationResponse.fromEntity(
                v,
                checkCountMap.getOrDefault(v.getId(), 0L),
                requiredChecks,
                viewerUserId,
                checkedByMe.contains(v.getId()))));

        return responses;
    }
}
