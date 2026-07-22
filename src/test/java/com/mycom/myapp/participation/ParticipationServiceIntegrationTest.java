package com.mycom.myapp.participation;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.challenge.service.ParticipationService;
import com.mycom.myapp.common.exception.DuplicateParticipationException;
import com.mycom.myapp.common.exception.InsufficientPointException;
import com.mycom.myapp.user.entity.Role;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

@SpringBootTest
@Transactional // 테스트 완료 후 DB 데이터 자동 롤백
class ParticipationServiceIntegrationTest {

    @Autowired
    private ParticipationService participationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private ParticipationRepository participationRepository;

    private User testUser;
    private Challenge testChallenge;

    @BeforeEach
    void setUp() {
        // 1. User 엔티티 생성 및 DB 실제 영속화
        // User 클래스에 @Builder가 없으므로 기본 생성자 + Setter로 필수 필드 설정
        testUser = new User();
        testUser.setEmail("test@naver.com");
        testUser.setPassword("encryptedPassword123!");
        testUser.setName("테스트유저");
        testUser.setRole(Role.USER); // enum Role 선언 상태에 맞게 적용
        testUser.setPointBalance(10000); // 보증금(5,000원)보다 여유 있는 포인트 설정
        userRepository.save(testUser);

        // 2. Challenge 엔티티 생성 및 DB 실제 영속화
        // createdAt 컬럼이 nullable = false 이므로 builder 호출 시 LocalDateTime.now() 필수 주입
        testChallenge = Challenge.builder()
        		.host(testUser)
                .title("알고리즘 1일 1풀")
                .depositAmount(5000)
                .status(ChallengeStatus.RECRUITING)
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(3).plusWeeks(1))
                .createdAt(LocalDateTime.now())
                .build();
        challengeRepository.save(testChallenge);
    }

    @Test
    @DisplayName("통합 테스트: 정상 참여 시 DB에 Participation 레코드가 적재되고 식별자가 반환된다")
    void participate_success_integration() {
        // when
        Long participationId = participationService.participate(testChallenge.getId(), testUser.getUserId());

        // then
        assertThat(participationId).isNotNull();

        // 실제 DB 조회 및 저장 데이터 검증
        Participation savedParticipation = participationRepository.findById(participationId).orElse(null);
        assertThat(savedParticipation).isNotNull();
        assertThat(savedParticipation.getUser().getUserId()).isEqualTo(testUser.getUserId());
        assertThat(savedParticipation.getChallenge().getId()).isEqualTo(testChallenge.getId());
    }

    @Test
    @DisplayName("통합 테스트: 중복 참여 시 DB Unique 제약조건/1차 검증에 의해 DuplicateParticipationException이 격발된다")
    void participate_duplicate_integration() {
        // given (1회차 정상 참여)
        participationService.participate(testChallenge.getId(), testUser.getUserId());

        // when & then (동일 유저 2회차 연속 참여 시도)
        assertThatThrownBy(() -> participationService.participate(testChallenge.getId(), testUser.getUserId()))
                .isInstanceOf(DuplicateParticipationException.class);
    }

    @Test
    @DisplayName("통합 테스트: 잔액 부족 시 InsufficientPointException이 발생하고 DB에 레코드가 적재되지 않는다")
    void participate_insufficient_point_integration() {
        // given (보증금 5,000원보다 적은 1,000원 보유 유저 생성 및 저장)
        User poorUser = new User();
        poorUser.setEmail("poor@naver.com");
        poorUser.setPassword("encryptedPassword123!");
        poorUser.setName("잔액부족유저");
        poorUser.setRole(Role.USER);
        poorUser.setPointBalance(1000);
        userRepository.save(poorUser);

        // when & then
        assertThatThrownBy(() -> participationService.participate(testChallenge.getId(), poorUser.getUserId()))
                .isInstanceOf(InsufficientPointException.class);

        // DB에 실제로 저장된 레코드가 없는지 검증
        boolean exists = participationRepository.existsByChallenge_IdAndUser_UserId(testChallenge.getId(), poorUser.getUserId());
        assertThat(exists).isFalse();
    }
}