package com.mycom.myapp.participation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.challenge.service.ChallengeService;
import com.mycom.myapp.challenge.service.ParticipationServiceImpl;
import com.mycom.myapp.common.exception.DuplicateParticipationException;
import com.mycom.myapp.common.exception.InsufficientPointException;
import com.mycom.myapp.point.service.PointService;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class) // Spring을 띄우지 않고 Mockito 프레임워크만 사용
@Slf4j
public class ParticipationServiceUnitTest {

	@InjectMocks
    private ParticipationServiceImpl participationService; // 테스트할 대상 (Mock들이 자동으로 주입됨)

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChallengeService challengeService;

    @Mock
    private PointService pointService;

    @Test
    @DisplayName("챌린지 참여 성공 - 정상 조건시 포인트 잠금 및 참여 정보가 저장된다")
    void participate_success() {
        // given (테스트 환경 및 가짜 데이터 준비)
        Long challengeId = 1L;
        Long userId = 1L;

        // 1. 중복 참여가 아니라고 가설 설정
        given(participationRepository.existsByChallenge_IdAndUser_UserId(challengeId, userId))
                .willReturn(false);

        // 2. 가짜 유저 (잔액 10,000원)
        User mockUser = mock(User.class);
        given(mockUser.getPointBalance()).willReturn(10000);
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

        // 3. 가짜 챌린지 (모집중, 보증금 5,000원)
        Challenge mockChallenge = mock(Challenge.class);
        given(mockChallenge.getStatus()).willReturn(ChallengeStatus.RECRUITING);
        given(mockChallenge.getDepositAmount()).willReturn(5000);
        given(challengeService.getValidChallenge(challengeId)).willReturn(mockChallenge);

        // when (실제 메서드 실행)
        participationService.participate(challengeId, userId);

        // then (결과 검증)
        // PointService의 lockPoint()가 5,000원으로 호출되었는지 검증
        then(pointService).should().lockPoint(userId, 5000);
        
        // ParticipationRepository의 save()가 실행되었는지 검증
        then(participationRepository).should().save(any(Participation.class));
    }
    
    @Test
    @DisplayName("잔액 부족 예외 - 보유 포인트가 보증금보다 적으면 InsufficientPointException이 발생한다")
    void participate_insufficient_point_exception() {
        // given
        Long challengeId = 1L;
        Long userId = 1L;

        // 1. 중복 참여 검증 통과 (false)
        given(participationRepository.existsByChallenge_IdAndUser_UserId(challengeId, userId))
                .willReturn(false);

        // 2. 가짜 유저 준비 (보유 포인트: 1,000원)
        User mockUser = mock(User.class);
        given(mockUser.getPointBalance()).willReturn(1000);
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

        // 3. 가짜 챌린지 준비 (모집중, 필요 보증금: 5,000원)
        Challenge mockChallenge = mock(Challenge.class);
        given(mockChallenge.getStatus()).willReturn(ChallengeStatus.RECRUITING);
        given(mockChallenge.getDepositAmount()).willReturn(5000);
        given(challengeService.getValidChallenge(challengeId)).willReturn(mockChallenge);

        // when & then
        // 유저 포인트(1,000원) < 필요 보증금(5,000원) 이므로 InsufficientPointException 격발 검증
        assertThatThrownBy(() -> participationService.participate(challengeId, userId))
                .isInstanceOf(InsufficientPointException.class);
    }

    @Test
    @DisplayName("중복 참여 예외 - 이미 참여한 유저면 DuplicateParticipationException이 발생한다")
    void participate_duplicate_exception() {
        // given
        Long challengeId = 1L;
        Long userId = 1L;

        // 이미 참여중(true)으로 가설 설정
        given(participationRepository.existsByChallenge_IdAndUser_UserId(challengeId, userId))
                .willReturn(true);

        // when & then
        // 메서드 실행 시 예외가 발생하는지 확인
        assertThatThrownBy(() -> participationService.participate(challengeId, userId))
                .isInstanceOf(DuplicateParticipationException.class);
    }
}
