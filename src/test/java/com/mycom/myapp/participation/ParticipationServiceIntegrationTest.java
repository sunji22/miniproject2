package com.mycom.myapp.participation;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.dto.ParticipantResponseDto;
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
    
    @Test
    void 참여자_목록_조회_서비스_통합테스트() {
    	// (given)
    	// 1. 50명의 유저 생성 및 저장
        List<User> users = IntStream.rangeClosed(1, 50)
                .mapToObj(i -> {
                	User user = new User();
                	user.setEmail("test" + i + "@naver.com");
                	user.setPassword("encryptedPassword123!");
                	user.setName("테스트유저" + i);
                	user.setRole(Role.USER);
                	user.setPointBalance(10000);
                	return user;
                })
                .toList();
        userRepository.saveAll(users);

        // 2. 챌린지 엔티티 생성 및 저장
        Challenge challenge = Challenge.builder()
					        		.host(users.get(0))
					                .title("매일 스쿼트 100개")
					                .depositAmount(5000)
					                .status(ChallengeStatus.RECRUITING)
					                .startDate(LocalDate.now().plusDays(3))
					                .endDate(LocalDate.now().plusDays(3).plusWeeks(1))
					                .createdAt(LocalDateTime.now())
					                .build();
        challengeRepository.save(challenge);

        // 3. 100명의 유저를 챌린지에 참여 엔티티로 매핑 및 저장
        List<Participation> participations = users.stream()
                .map(user -> Participation.createParticipation(user, challenge))
                .toList();
        participationRepository.saveAll(participations);
        
        // (when)
        List<ParticipantResponseDto> result = participationService.listParticipant(challenge.getId());
        
        // (then)
        // 1. 전체 조회 결과 리스트의 Null 여부 및 개수 검증 (50명)
        assertThat(result)
                .isNotNull()
                .hasSize(50);

        // 2. DTO에 담긴 참여자 정보(이름)가 실제 생성된 데이터와 일치하는지 검증
        assertThat(result)
                .extracting("userName") // ParticipantResponseDto의 유저 이름 필드명 (필요시 필드명/Getter 수정)
                .contains("테스트유저1", "테스트유저25", "테스트유저50");

        // 3. (Edge Case) 참여자가 없는/존재하지 않는 챌린지 ID 조회 시 빈 리스트 반환 검증
        Long invalidChallengeId = 9999L;
        List<ParticipantResponseDto> emptyResult = participationService.listParticipant(invalidChallengeId);
        
        assertThat(emptyResult)
                .isNotNull()
                .isEmpty();
    }
}