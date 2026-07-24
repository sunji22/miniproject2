package com.mycom.myapp.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.challenge.service.ChallengeServiceImpl;
import com.mycom.myapp.challenge.service.ParticipationService;
import com.mycom.myapp.common.exception.CannotDeleteOngoingChallengeException;
import com.mycom.myapp.common.exception.ExceededRequiredCountException;
import com.mycom.myapp.common.exception.InvalidChallengePeriodException;
import com.mycom.myapp.common.exception.InvalidChallengeStatusException;
import com.mycom.myapp.common.exception.NotChallengeHostException;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceUnitTest {

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private ParticipationService participationService;

    private User hostUser;
    private Challenge challenge;
    private ChallengeDto challengeDto;

    @BeforeEach
    void setUp() {
        hostUser = new User();
        hostUser.setUserId(1L);
        hostUser.setName("주최자");

        challenge = Challenge.builder()
                .id(100L)
                .title("기존 챌린지")
                .description("설명")
                .status(ChallengeStatus.RECRUITING)
                .host(hostUser)
                .build();

        challengeDto = new ChallengeDto();
        challengeDto.setTitle("신규 챌린지");
        challengeDto.setDescription("신규 설명");
        challengeDto.setStartDate(LocalDate.now().plusDays(1));
        challengeDto.setEndDate(LocalDate.now().plusDays(7)); // 7일간
        challengeDto.setRequiredCount(5); // 5회 인증
        challengeDto.setDepositAmount(10000);
    }

    // ================= [ insertChallenge 테스트 ] =================

    @Test
    @DisplayName("챌린지 생성 성공 - 시작일/인증횟수 검증 통과 시 생성 및 개설자 자동 참여 호출")
    void insertChallenge_Success() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(hostUser));
        given(challengeRepository.save(any(Challenge.class))).willReturn(challenge);

        // when
        Long challengeId = challengeService.insertChallenge(challengeDto, 1L);

        // then
        assertThat(challengeId).isEqualTo(100L);
        verify(challengeRepository).save(any(Challenge.class));
        verify(participationService).participate(100L, 1L); // 개설자 자동 참여 검증
    }

    @Test
    @DisplayName("챌린지 생성 실패 - 시작일이 종료일보다 늦은 경우 InvalidChallengePeriodException 발생")
    void insertChallenge_Exception_InvalidPeriod() {
        // given
        challengeDto.setStartDate(LocalDate.now().plusDays(10));
        challengeDto.setEndDate(LocalDate.now().plusDays(5)); // 시작일 > 종료일

        // when & then
        assertThatThrownBy(() -> challengeService.insertChallenge(challengeDto, 1L))
                .isInstanceOf(InvalidChallengePeriodException.class);

        verify(challengeRepository, never()).save(any());
    }

    @Test
    @DisplayName("챌린지 생성 실패 - 인증 횟수가 총 기간(일)을 초과할 경우 ExceededRequiredCountException 발생")
    void insertChallenge_Exception_ExceededRequiredCount() {
        // given (기간: 3일 / 인증 필요: 5회)
        challengeDto.setStartDate(LocalDate.now().plusDays(1));
        challengeDto.setEndDate(LocalDate.now().plusDays(3));
        challengeDto.setRequiredCount(5);

        // when & then
        assertThatThrownBy(() -> challengeService.insertChallenge(challengeDto, 1L))
                .isInstanceOf(ExceededRequiredCountException.class);

        verify(challengeRepository, never()).save(any());
    }

    // ================= [ updateChallenge 테스트 ] =================

    @Test
    @DisplayName("챌린지 수정 성공 - 주최자 권한 및 모집 중 상태 검증 통과 시 변경 감지(Dirty Checking) 동작")
    void updateChallenge_Success() {
        // given
        challengeDto.setId(100L);
        given(challengeRepository.findById(100L)).willReturn(Optional.of(challenge));

        // when
        Long updatedId = challengeService.updateChallenge(challengeDto, 1L);

        // then
        assertThat(updatedId).isEqualTo(100L);
        assertThat(challenge.getTitle()).isEqualTo("신규 챌린지");
    }

    @Test
    @DisplayName("챌린지 수정 실패 - 요청자가 주최자가 아닌 경우 NotChallengeHostException 발생")
    void updateChallenge_Exception_NotHost() {
        // given
        challengeDto.setId(100L);
        Long otherUserId = 999L; // 타인
        given(challengeRepository.findById(100L)).willReturn(Optional.of(challenge));

        // when & then
        assertThatThrownBy(() -> challengeService.updateChallenge(challengeDto, otherUserId))
                .isInstanceOf(NotChallengeHostException.class);
    }

    @Test
    @DisplayName("챌린지 수정 실패 - 이미 모집중(RECRUITING) 상태가 아닌 경우 InvalidChallengeStatusException 발생")
    void updateChallenge_Exception_InvalidStatus() {
        // given
        challengeDto.setId(100L);
        Challenge ongoingChallenge = Challenge.builder()
                .id(100L)
                .status(ChallengeStatus.ONGOING) // 진행 중
                .host(hostUser)
                .build();

        given(challengeRepository.findById(100L)).willReturn(Optional.of(ongoingChallenge));

        // when & then
        assertThatThrownBy(() -> challengeService.updateChallenge(challengeDto, 1L))
                .isInstanceOf(InvalidChallengeStatusException.class);
    }

    // ================= [ deleteChallenge 테스트 ] =================

    @Test
    @DisplayName("챌린지 삭제 실패 - 진행 중(ONGOING)인 챌린지 삭제 시 CannotDeleteOngoingChallengeException 발생")
    void deleteChallenge_Exception_Ongoing() {
        // given
        Challenge ongoingChallenge = Challenge.builder()
                .id(100L)
                .status(ChallengeStatus.ONGOING)
                .host(hostUser)
                .build();

        given(challengeRepository.findById(100L)).willReturn(Optional.of(ongoingChallenge));

        // when & then
        assertThatThrownBy(() -> challengeService.deleteChallenge(100L, 1L))
                .isInstanceOf(CannotDeleteOngoingChallengeException.class);

        verify(challengeRepository, never()).delete(any());
    }
}