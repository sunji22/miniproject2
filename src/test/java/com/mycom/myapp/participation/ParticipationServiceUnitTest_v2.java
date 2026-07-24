package com.mycom.myapp.participation;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.entity.ParticipationStatus;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.challenge.service.ParticipationServiceImpl;
import com.mycom.myapp.common.exception.DuplicateParticipationException;
import com.mycom.myapp.common.exception.InsufficientPointException;
import com.mycom.myapp.common.exception.InvalidChallengeStatusException;
import com.mycom.myapp.point.service.PointService;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceUnitTest_v2 {

    @InjectMocks
    private ParticipationServiceImpl participationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private PointService pointService;

    private User user;
    private Challenge challenge;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setPointBalance(10000);

        challenge = Challenge.builder()
                .id(100L)
                .depositAmount(5000)
                .status(ChallengeStatus.RECRUITING)
                .build();
    }

    @Test
    @DisplayName("신규 참여 성공 - 최초 참여 시 엔티티 생성 및 포인트 잠금 처리")
    void participate_Success_NewParticipation() {
        // (given)
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(challengeRepository.findById(100L)).willReturn(Optional.of(challenge));
        given(participationRepository.findByChallenge_IdAndUser_UserId(100L, 1L)).willReturn(Optional.empty());

        Participation savedParticipation = Participation.createParticipation(user, challenge);
        // Reflection API 등으로 테스트용 ID 세팅
        ReflectionTestUtils.setField(savedParticipation, "id", 10L);
        given(participationRepository.save(any(Participation.class))).willReturn(savedParticipation);

        // (when)
        Long resultId = participationService.participate(100L, 1L);

        // (then)
        assertThat(resultId).isEqualTo(10L);
        // pointService 객체의 lockPoint 메서드가 첫 번째 인자로 1L, 두번째로 Participation 타입의 객체, 세 번째로 5000이라는 정확한 수치를 가지고 1회 호출되었는지 검증
        verify(pointService).lockPoint(eq(1L), any(Participation.class), eq(5000)); // 보증금 잠금 실행 검증
        verify(participationRepository).save(any(Participation.class));
    }

    @Test
    @DisplayName("재참여 성공 - CANCLED 상태인 기존 참여 정보를 JOINED 상태로 변경")
    void participate_Success_Rejoin() {
        // (given)
        Participation canceledParticipation = Participation.createParticipation(user, challenge);
        canceledParticipation.cancel(); // 상태를 CANCLED로 변경
        ReflectionTestUtils.setField(canceledParticipation, "id", 10L, Long.class);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(challengeRepository.findById(100L)).willReturn(Optional.of(challenge));
        given(participationRepository.findByChallenge_IdAndUser_UserId(100L, 1L)).willReturn(Optional.of(canceledParticipation));

        // (when)
        Long resultId = participationService.participate(100L, 1L);

        // (then)
        assertThat(resultId).isEqualTo(10L);
        assertThat(canceledParticipation.getStatus()).isEqualTo(ParticipationStatus.JOINED); // 상태 전이 검증
        verify(pointService).lockPoint(eq(1L), any(Participation.class), eq(5000));
        verify(participationRepository, never()).save(any()); // 기존 객체 변경이므로 save 미호출
    }

    @Test
    @DisplayName("예외 - 이미 JOINED 상태로 참여 중인 경우 DuplicateParticipationException 발생")
    void participate_Exception_AlreadyJoined() {
        // (given)
        Participation activeParticipation = Participation.createParticipation(user, challenge); // 기본 JOINED

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(challengeRepository.findById(100L)).willReturn(Optional.of(challenge));
        given(participationRepository.findByChallenge_IdAndUser_UserId(100L, 1L)).willReturn(Optional.of(activeParticipation));

        // (when & then)
        assertThatThrownBy(() -> participationService.participate(100L, 1L))
                .isInstanceOf(DuplicateParticipationException.class);

        verify(pointService, never()).lockPoint(anyLong(), any(Participation.class), anyInt());
    }

    @Test
    @DisplayName("예외 - 모집 중(RECRUITING)이 아닌 챌린지일 경우 InvalidChallengeStatusException 발생")
    void participate_Exception_NotRecruiting() {
        // (given)
        Challenge ongoingChallenge = Challenge.builder()
                .id(100L)
                .depositAmount(5000)
                .status(ChallengeStatus.ONGOING) // 진행 중 상태
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(challengeRepository.findById(100L)).willReturn(Optional.of(ongoingChallenge));
        given(participationRepository.findByChallenge_IdAndUser_UserId(100L, 1L)).willReturn(Optional.empty());

        // (when & then)
        assertThatThrownBy(() -> participationService.participate(100L, 1L))
                .isInstanceOf(InvalidChallengeStatusException.class);
    }

    @Test
    @DisplayName("예외 - 보유 포인트가 보증금보다 부족할 경우 InsufficientPointException 발생")
    void participate_Exception_InsufficientPoint() {
        // (given)
        user.setPointBalance(3000); // 보증금(5000)보다 적음

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(challengeRepository.findById(100L)).willReturn(Optional.of(challenge));
        given(participationRepository.findByChallenge_IdAndUser_UserId(100L, 1L)).willReturn(Optional.empty());

        // (when & then)
        assertThatThrownBy(() -> participationService.participate(100L, 1L))
                .isInstanceOf(InsufficientPointException.class);

        verify(pointService, never()).lockPoint(anyLong(), any(Participation.class), anyInt());
    }

    @Test
    @DisplayName("예외 - 동시 요청으로 인한 DB Unique 제약조건 위반 시 DuplicateParticipationException 예외로 래핑")
    void participate_Exception_ConcurrencyDbConstraintViolation() {
        // (given)
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(challengeRepository.findById(100L)).willReturn(Optional.of(challenge));
        given(participationRepository.findByChallenge_IdAndUser_UserId(100L, 1L)).willReturn(Optional.empty());

        // save 시점에 DataIntegrityViolationException 발생 모킹
        given(participationRepository.save(any(Participation.class)))
                .willThrow(new DataIntegrityViolationException("Unique 제약조건 위반"));

        // (when & then)
        assertThatThrownBy(() -> participationService.participate(100L, 1L))
                .isInstanceOf(DuplicateParticipationException.class);
    }
}