package com.mycom.myapp.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.common.exception.AlreadyCheckedException;
import com.mycom.myapp.common.exception.DuplicateVerificationException;
import com.mycom.myapp.common.exception.NotChallengeParticipantException;
import com.mycom.myapp.common.exception.NotVerificationOwnerException;
import com.mycom.myapp.common.exception.SelfCheckNotAllowedException;
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

// 인증글 서비스 단위테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationServiceImpl - 인증글 등록 / 수정 / 조회 / 상호체크 / 성공 판정")
class VerificationServiceImplTest {

    @Mock
    private VerificationRepository verificationRepository;
    @Mock
    private VerificationCheckRepository verificationCheckRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    private static final Long CHALLENGE_ID = 6L;
    private static final Long PARTICIPATION_ID = 9L;
    private static final Long VERIFICATION_ID = 100L;
    private static final Long WRITER_ID = 2L;
    private static final Long CHECKER_ID = 6L;

    private Challenge challenge;
    private Participation participation;
    private User writer;

    @BeforeEach
    void setUp() {
        challenge = Challenge.builder()
                .id(CHALLENGE_ID)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(5))
                .requiredCount(21)
                .depositAmount(30000)
                .build();

        writer = new User();
        writer.setUserId(WRITER_ID);
        writer.setName("홍길동");

        participation = new Participation();
        participation.setId(PARTICIPATION_ID);
        participation.setChallenge(challenge);
        participation.setUser(writer);
        participation.setSuccessCount(0);
    }

    private VerificationCreateRequest createRequest() {
        return new VerificationCreateRequest(CHALLENGE_ID, "https://cdn.example.com/v/1.png", "오늘치 완료");
    }

    private Verification savedVerification(boolean succeeded) {
        Verification v = new Verification();
        v.setId(VERIFICATION_ID);
        v.setParticipation(participation);
        v.setUser(writer);
        v.setImageUrl("https://cdn.example.com/v/1.png");
        v.setContent("오늘치 완료");
        v.setVerifiedDate(LocalDate.now());
        v.setSucceeded(succeeded);
        return v;
    }

    private User checker() {
        User checker = new User();
        checker.setUserId(CHECKER_ID);
        checker.setName("최지우");
        return checker;
    }

    // 저장 시 id 를 채워 돌려주는 목
    private void givenSaveReturnsWithId(Long id) {
        given(verificationRepository.save(any(Verification.class)))
                .willAnswer(invocation -> {
                    Verification v = invocation.getArgument(0);
                    v.setId(id);
                    return v;
                });
    }

    // createVerification
    @Test
    @DisplayName("정상 등록 - 참여자 3명이면 succeeded=false, checkCount=0, requiredChecks=2")
    void createVerification_success() {
        given(participationRepository.findByChallenge_IdAndUser_UserId(CHALLENGE_ID, WRITER_ID))
                .willReturn(Optional.of(participation));
        given(verificationRepository.existsByParticipation_IdAndVerifiedDate(PARTICIPATION_ID, LocalDate.now()))
                .willReturn(false);
        given(userRepository.findById(WRITER_ID)).willReturn(Optional.of(writer));
        given(participationRepository.countByChallenge_Id(CHALLENGE_ID)).willReturn(3L);
        givenSaveReturnsWithId(VERIFICATION_ID);

        VerificationResponse response = verificationService.createVerification(createRequest(), WRITER_ID);

        assertThat(response.getId()).isEqualTo(VERIFICATION_ID);
        assertThat(response.isSucceeded()).isFalse();
        assertThat(response.getCheckCount()).isZero();
        assertThat(response.getRequiredChecks()).isEqualTo(2L);
        assertThat(response.getVerifiedDate()).isEqualTo(LocalDate.now());
        assertThat(response.isMine()).isTrue();          // 작성자가 곧 조회자
        assertThat(participation.getSuccessCount()).isZero();
    }

    @Test
    @DisplayName("참여하지 않은 챌린지에 인증글 -> NotChallengeParticipantException")
    void createVerification_notParticipant() {
        given(participationRepository.findByChallenge_IdAndUser_UserId(CHALLENGE_ID, WRITER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.createVerification(createRequest(), WRITER_ID))
                .isInstanceOf(NotChallengeParticipantException.class);

        verify(verificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("챌린지 기간 밖 -> VerificationPeriodException")
    void createVerification_outOfPeriod() {
        challenge.setStartDate(LocalDate.now().minusDays(30));
        challenge.setEndDate(LocalDate.now().minusDays(1));     // 이미 끝난 챌린지

        given(participationRepository.findByChallenge_IdAndUser_UserId(CHALLENGE_ID, WRITER_ID))
                .willReturn(Optional.of(participation));

        assertThatThrownBy(() -> verificationService.createVerification(createRequest(), WRITER_ID))
                .isInstanceOf(VerificationPeriodException.class);

        verify(verificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 날 두 번째 인증글 -> DuplicateVerificationException")
    void createVerification_duplicateOnSameDay() {
        given(participationRepository.findByChallenge_IdAndUser_UserId(CHALLENGE_ID, WRITER_ID))
                .willReturn(Optional.of(participation));
        given(verificationRepository.existsByParticipation_IdAndVerifiedDate(PARTICIPATION_ID, LocalDate.now()))
                .willReturn(true);

        assertThatThrownBy(() -> verificationService.createVerification(createRequest(), WRITER_ID))
                .isInstanceOf(DuplicateVerificationException.class);

        verify(verificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("참여자 1명(체크해 줄 사람 없음) -> 등록 즉시 succeeded=true, successCount=1")
    void createVerification_soloChallengeSucceedsImmediately() {
        given(participationRepository.findByChallenge_IdAndUser_UserId(CHALLENGE_ID, WRITER_ID))
                .willReturn(Optional.of(participation));
        given(verificationRepository.existsByParticipation_IdAndVerifiedDate(PARTICIPATION_ID, LocalDate.now()))
                .willReturn(false);
        given(userRepository.findById(WRITER_ID)).willReturn(Optional.of(writer));
        given(participationRepository.countByChallenge_Id(CHALLENGE_ID)).willReturn(1L);
        givenSaveReturnsWithId(101L);

        VerificationResponse response = verificationService.createVerification(createRequest(), WRITER_ID);

        assertThat(response.getRequiredChecks()).isZero();
        assertThat(response.isSucceeded()).isTrue();
        assertThat(participation.getSuccessCount()).isEqualTo(1);
    }

    // updateVerification
    private VerificationUpdateRequest updateRequest() {
        return new VerificationUpdateRequest("https://cdn.example.com/v/fixed.png", "사진 다시 올림");
    }

    @Test
    @DisplayName("정상 수정 - 아직 체크 0건이면 imageUrl/content 가 바뀐다")
    void updateVerification_success() {
        Verification verification = savedVerification(false);

        given(verificationRepository.findById(VERIFICATION_ID)).willReturn(Optional.of(verification));
        given(verificationCheckRepository.existsByVerification_Id(VERIFICATION_ID)).willReturn(false);
        given(participationRepository.countByChallenge_Id(CHALLENGE_ID)).willReturn(3L);

        VerificationResponse response =
                verificationService.updateVerification(VERIFICATION_ID, updateRequest(), WRITER_ID);

        // 더티체킹으로 반영되므로 save 호출은 없어야 한다
        verify(verificationRepository, never()).save(any());
        assertThat(verification.getImageUrl()).isEqualTo("https://cdn.example.com/v/fixed.png");
        assertThat(verification.getContent()).isEqualTo("사진 다시 올림");
        assertThat(response.getImageUrl()).isEqualTo("https://cdn.example.com/v/fixed.png");
    }

    @Test
    @DisplayName("남의 인증글 수정 -> NotVerificationOwnerException")
    void updateVerification_notOwner() {
        Verification verification = savedVerification(false);

        given(verificationRepository.findById(VERIFICATION_ID)).willReturn(Optional.of(verification));

        assertThatThrownBy(() ->
                verificationService.updateVerification(VERIFICATION_ID, updateRequest(), CHECKER_ID))
                .isInstanceOf(NotVerificationOwnerException.class);

        // 원본이 그대로
        assertThat(verification.getImageUrl()).isEqualTo("https://cdn.example.com/v/1.png");
    }

    @Test
    @DisplayName("이미 체크받은 인증글 수정 -> VerificationAlreadyCheckedModifyException")
    void updateVerification_alreadyChecked() {
        Verification verification = savedVerification(false);

        given(verificationRepository.findById(VERIFICATION_ID)).willReturn(Optional.of(verification));
        given(verificationCheckRepository.existsByVerification_Id(VERIFICATION_ID)).willReturn(true);

        assertThatThrownBy(() ->
                verificationService.updateVerification(VERIFICATION_ID, updateRequest(), WRITER_ID))
                .isInstanceOf(VerificationAlreadyCheckedModifyException.class);

        assertThat(verification.getImageUrl()).isEqualTo("https://cdn.example.com/v/1.png");
    }

    @Test
    @DisplayName("없는 인증글 수정 -> VerificationNotFoundException")
    void updateVerification_notFound() {
        given(verificationRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                verificationService.updateVerification(999L, updateRequest(), WRITER_ID))
                .isInstanceOf(VerificationNotFoundException.class);
    }

    // checkVerification
    @Test
    @DisplayName("체크 1건인데 정원은 2건 -> succeeded 유지 false, successCount 불변")
    void checkVerification_notEnoughChecks() {
        Verification verification = savedVerification(false);

        given(verificationRepository.findById(VERIFICATION_ID)).willReturn(Optional.of(verification));
        given(participationRepository.existsByChallenge_IdAndUser_UserId(CHALLENGE_ID, CHECKER_ID))
                .willReturn(true);
        given(verificationCheckRepository.existsByVerification_IdAndChecker_UserId(VERIFICATION_ID, CHECKER_ID))
                .willReturn(false);
        given(userRepository.findById(CHECKER_ID)).willReturn(Optional.of(checker()));
        given(verificationCheckRepository.countByVerification_Id(VERIFICATION_ID)).willReturn(1L);
        given(participationRepository.countByChallenge_Id(CHALLENGE_ID)).willReturn(3L); // 정원 2

        verificationService.checkVerification(VERIFICATION_ID, CHECKER_ID);

        verify(verificationCheckRepository).save(any(VerificationCheck.class));
        assertThat(verification.isSucceeded()).isFalse();
        assertThat(participation.getSuccessCount()).isZero();
    }

    @Test
    @DisplayName("마지막 체크로 정원 충족 -> succeeded=true, successCount++")
    void checkVerification_reachesRequiredChecks() {
        Verification verification = savedVerification(false);

        given(verificationRepository.findById(VERIFICATION_ID)).willReturn(Optional.of(verification));
        given(participationRepository.existsByChallenge_IdAndUser_UserId(CHALLENGE_ID, CHECKER_ID))
                .willReturn(true);
        given(verificationCheckRepository.existsByVerification_IdAndChecker_UserId(VERIFICATION_ID, CHECKER_ID))
                .willReturn(false);
        given(userRepository.findById(CHECKER_ID)).willReturn(Optional.of(checker()));
        given(verificationCheckRepository.countByVerification_Id(VERIFICATION_ID)).willReturn(2L);
        given(participationRepository.countByChallenge_Id(CHALLENGE_ID)).willReturn(3L); // 정원 2

        verificationService.checkVerification(VERIFICATION_ID, CHECKER_ID);

        assertThat(verification.isSucceeded()).isTrue();
        assertThat(participation.getSuccessCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("본인이 쓴 인증글 자가 체크 -> SelfCheckNotAllowedException")
    void checkVerification_selfCheck() {
        Verification verification = savedVerification(false);

        given(verificationRepository.findById(VERIFICATION_ID)).willReturn(Optional.of(verification));
        given(participationRepository.existsByChallenge_IdAndUser_UserId(CHALLENGE_ID, WRITER_ID))
                .willReturn(true);

        assertThatThrownBy(() -> verificationService.checkVerification(VERIFICATION_ID, WRITER_ID))
                .isInstanceOf(SelfCheckNotAllowedException.class);

        verify(verificationCheckRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 체크한 사람이 또 체크 -> AlreadyCheckedException")
    void checkVerification_alreadyChecked() {
        Verification verification = savedVerification(false);

        given(verificationRepository.findById(VERIFICATION_ID)).willReturn(Optional.of(verification));
        given(participationRepository.existsByChallenge_IdAndUser_UserId(CHALLENGE_ID, CHECKER_ID))
                .willReturn(true);
        given(verificationCheckRepository.existsByVerification_IdAndChecker_UserId(VERIFICATION_ID, CHECKER_ID))
                .willReturn(true);

        assertThatThrownBy(() -> verificationService.checkVerification(VERIFICATION_ID, CHECKER_ID))
                .isInstanceOf(AlreadyCheckedException.class);

        verify(verificationCheckRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 인증글 체크 -> VerificationNotFoundException")
    void checkVerification_notFound() {
        given(verificationRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.checkVerification(999L, CHECKER_ID))
                .isInstanceOf(VerificationNotFoundException.class);
    }

    @Test
    @DisplayName("이미 succeeded 인 인증글에 추가 체크 -> successCount 를 다시 올리지 않는다")
    void checkVerification_idempotentSuccessCount() {
        Verification verification = savedVerification(true);   // 이미 성공 판정 끝남
        participation.setSuccessCount(1);

        given(verificationRepository.findById(VERIFICATION_ID)).willReturn(Optional.of(verification));
        given(participationRepository.existsByChallenge_IdAndUser_UserId(CHALLENGE_ID, CHECKER_ID))
                .willReturn(true);
        given(verificationCheckRepository.existsByVerification_IdAndChecker_UserId(VERIFICATION_ID, CHECKER_ID))
                .willReturn(false);
        given(userRepository.findById(CHECKER_ID)).willReturn(Optional.of(checker()));
        given(verificationCheckRepository.countByVerification_Id(VERIFICATION_ID)).willReturn(3L);
        given(participationRepository.countByChallenge_Id(CHALLENGE_ID)).willReturn(3L);

        verificationService.checkVerification(VERIFICATION_ID, CHECKER_ID);

        // 체크 자체는 저장되지만 성공 카운트는 그대로
        verify(verificationCheckRepository).save(any(VerificationCheck.class));
        assertThat(participation.getSuccessCount()).isEqualTo(1);
    }

    // 조회
    @Test
    @DisplayName("챌린지 날짜별 피드 - checkCount/requiredChecks 와 mine/checkedByMe 플래그를 채운다")
    void listByChallenge_withDate() {
        Verification verification = savedVerification(false);
        LocalDate date = LocalDate.now();

        given(participationRepository.countByChallenge_Id(CHALLENGE_ID)).willReturn(3L);
        given(verificationRepository.findFeedByChallengeAndDate(CHALLENGE_ID, date))
                .willReturn(List.of(verification));
        // List.of(new Object[]{..}) 는 varargs 로 펼쳐져 List<Object> 로 추론된다 -> 타입 명시 필요
        given(verificationCheckRepository.countGroupedByVerificationIds(anyList()))
                .willReturn(List.<Object[]>of(new Object[]{VERIFICATION_ID, 1L}));
        // 조회자(CHECKER_ID)는 아직 이 글을 체크하지 않았다
        given(verificationCheckRepository.findCheckedVerificationIds(anyLong(), anyList()))
                .willReturn(List.of());

        List<VerificationResponse> responses =
                verificationService.listByChallenge(CHALLENGE_ID, date, CHECKER_ID);

        assertThat(responses).hasSize(1);
        VerificationResponse response = responses.getFirst();
        assertThat(response.getCheckCount()).isEqualTo(1L);
        assertThat(response.getRequiredChecks()).isEqualTo(2L);
        assertThat(response.getChallengeId()).isEqualTo(CHALLENGE_ID);
        assertThat(response.getUserName()).isEqualTo("홍길동");
        assertThat(response.isMine()).isFalse();         // 남의 글 -> 수정 버튼 숨김
        assertThat(response.isCheckedByMe()).isFalse();  // 아직 안 눌렀음 -> 체크 버튼 활성
    }

    @Test
    @DisplayName("피드 - 내가 이미 체크한 글은 checkedByMe=true, 체크 0건이면 checkCount=0")
    void listByChallenge_flagsPerViewer() {
        Verification verification = savedVerification(false);

        given(participationRepository.countByChallenge_Id(CHALLENGE_ID)).willReturn(3L);
        given(verificationRepository.findFeedByChallenge(CHALLENGE_ID))
                .willReturn(List.of(verification));
        // 체크가 0건이면 GROUP BY 결과에 아예 행이 없다 -> getOrDefault(0L) 경로
        given(verificationCheckRepository.countGroupedByVerificationIds(anyList()))
                .willReturn(List.of());
        given(verificationCheckRepository.findCheckedVerificationIds(anyLong(), anyList()))
                .willReturn(List.of(VERIFICATION_ID));

        List<VerificationResponse> responses =
                verificationService.listByChallenge(CHALLENGE_ID, null, CHECKER_ID);

        assertThat(responses.getFirst().getCheckCount()).isZero();
        assertThat(responses.getFirst().isCheckedByMe()).isTrue();
    }

    @Test
    @DisplayName("작성자 본인이 피드를 보면 mine=true (수정 버튼 노출)")
    void listByChallenge_mineFlag() {
        Verification verification = savedVerification(false);

        given(participationRepository.countByChallenge_Id(CHALLENGE_ID)).willReturn(3L);
        given(verificationRepository.findFeedByChallenge(CHALLENGE_ID))
                .willReturn(List.of(verification));
        given(verificationCheckRepository.countGroupedByVerificationIds(anyList()))
                .willReturn(List.of());
        given(verificationCheckRepository.findCheckedVerificationIds(anyLong(), anyList()))
                .willReturn(List.of());

        List<VerificationResponse> responses =
                verificationService.listByChallenge(CHALLENGE_ID, null, WRITER_ID);

        assertThat(responses.getFirst().isMine()).isTrue();
    }

    @Test
    @DisplayName("인증글이 없으면 빈 목록")
    void listByChallenge_empty() {
        given(participationRepository.countByChallenge_Id(CHALLENGE_ID)).willReturn(3L);
        given(verificationRepository.findFeedByChallenge(CHALLENGE_ID)).willReturn(List.of());

        List<VerificationResponse> responses =
                verificationService.listByChallenge(CHALLENGE_ID, null, CHECKER_ID);

        assertThat(responses).isEmpty();
        verify(verificationCheckRepository, never()).countGroupedByVerificationIds(anyList());
    }

    @Test
    @DisplayName("단건 조회 - checkCount/requiredChecks/checkedByMe 를 채운다")
    void detailVerification_success() {
        Verification verification = savedVerification(false);

        given(verificationRepository.findById(VERIFICATION_ID)).willReturn(Optional.of(verification));
        given(verificationCheckRepository.countByVerification_Id(VERIFICATION_ID)).willReturn(1L);
        given(participationRepository.countByChallenge_Id(CHALLENGE_ID)).willReturn(3L);
        given(verificationCheckRepository.existsByVerification_IdAndChecker_UserId(VERIFICATION_ID, CHECKER_ID))
                .willReturn(true);

        VerificationResponse response = verificationService.detailVerification(VERIFICATION_ID, CHECKER_ID);

        assertThat(response.getCheckCount()).isEqualTo(1L);
        assertThat(response.getRequiredChecks()).isEqualTo(2L);
        assertThat(response.isCheckedByMe()).isTrue();
        assertThat(response.isMine()).isFalse();
    }

    @Test
    @DisplayName("없는 인증글 단건 조회 -> VerificationNotFoundException")
    void detailVerification_notFound() {
        given(verificationRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.detailVerification(999L, CHECKER_ID))
                .isInstanceOf(VerificationNotFoundException.class);
    }

    @Test
    @DisplayName("참여자 이력 조회 - 인증글이 없으면 빈 목록")
    void listByParticipation_empty() {
        given(verificationRepository.findFeedByParticipation(PARTICIPATION_ID)).willReturn(List.of());

        List<VerificationResponse> responses =
                verificationService.listByParticipation(PARTICIPATION_ID, null, CHECKER_ID);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("참여자 이력 조회 - 첫 건에서 challengeId 를 꺼내 requiredChecks 를 계산")
    void listByParticipation_success() {
        Verification verification = savedVerification(false);

        given(verificationRepository.findFeedByParticipation(PARTICIPATION_ID))
                .willReturn(List.of(verification));
        given(participationRepository.countByChallenge_Id(CHALLENGE_ID)).willReturn(3L);
        given(verificationCheckRepository.countGroupedByVerificationIds(anyList()))
                .willReturn(List.<Object[]>of(new Object[]{VERIFICATION_ID, 2L}));
        given(verificationCheckRepository.findCheckedVerificationIds(anyLong(), anyList()))
                .willReturn(List.of());

        List<VerificationResponse> responses =
                verificationService.listByParticipation(PARTICIPATION_ID, null, CHECKER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getParticipationId()).isEqualTo(PARTICIPATION_ID);
        assertThat(responses.getFirst().getCheckCount()).isEqualTo(2L);
        assertThat(responses.getFirst().getRequiredChecks()).isEqualTo(2L);
    }
}
