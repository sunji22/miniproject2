package com.mycom.myapp.participation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.user.entity.Role;
import com.mycom.myapp.user.entity.User;

import lombok.extern.slf4j.Slf4j;

@DataJpaTest // JPA 관련 Bean만 로딩하여 인메모리 DB(H2 등) 기반으로 빠르게 테스트
//🎯 실제 DB 설정(application.yml) 그대로 사용
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) 
@Slf4j
class ParticipationRepositoryTest {

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private TestEntityManager em; // 테스트 전용 EntityManager (픽스처 저장용)

    @Test
    @DisplayName("특정 유저 ID로 참여 목록을 조회한다")
    void findByUser_UserId() {
    	log.info("== 내 참여 목록 조회 테스트 ==");
    	
        // given
        User testUser = new User();
        testUser.setEmail("test@naver.com");
        testUser.setPassword("encryptedPassword123!");
        testUser.setName("테스트유저");
        testUser.setRole(Role.USER); // enum Role 선언 상태에 맞게 적용
        testUser.setPointBalance(10000); // 보증금(5,000원)보다 여유 있는 포인트 설정
        
        Challenge testChallenge1 = Challenge.builder()
        		.host(testUser)
                .title("챌린지1")
                .depositAmount(5000)
                .status(ChallengeStatus.RECRUITING)
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(3).plusWeeks(1))
                .createdAt(LocalDateTime.now())
                .build();
        Challenge testChallenge2 = Challenge.builder()
        		.host(testUser)
                .title("챌린지2")
                .depositAmount(5000)
                .status(ChallengeStatus.RECRUITING)
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(3).plusWeeks(1))
                .createdAt(LocalDateTime.now())
                .build();

        em.persist(testUser);
        em.persist(testChallenge1);
        em.persist(testChallenge2);

        Participation p1 = Participation.createParticipation(testUser, testChallenge1);
        Participation p2 = Participation.createParticipation(testUser, testChallenge2);

        em.persist(p1);
        em.persist(p2);
        em.flush();
        em.clear(); // 1차 캐시를 비워 실제 DB SELECT 쿼리 동작 검증

        // when
        List<Participation> results = participationRepository.findByUser_UserId(testUser.getUserId());

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(p -> p.getUser().getUserId())
                           .containsOnly(testUser.getUserId());
    }

    @Test
    @DisplayName("특정 챌린지 ID로 참여자 목록을 조회한다")
    void findByChallenge_Id() {
    	log.info("== 특정 챌린지의 참여자 목록 조회 테스트 ==");
    	
        // given
        User user1 = new User();
        user1.setEmail("test1@naver.com");
        user1.setPassword("encryptedPassword123!");
        user1.setName("테스트유저1");
        user1.setRole(Role.USER); // enum Role 선언 상태에 맞게 적용
        user1.setPointBalance(10000); // 보증금(5,000원)보다 여유 있는 포인트 설정
        
        User user2 = new User();
        user2.setEmail("test2@naver.com");
        user2.setPassword("encryptedPassword123!");
        user2.setName("테스트유저1");
        user2.setRole(Role.USER); // enum Role 선언 상태에 맞게 적용
        user2.setPointBalance(10000); // 보증금(5,000원)보다 여유 있는 포인트 설정
        
        // user1 이 개설한 챌린지
        Challenge challenge = Challenge.builder()
        		.host(user1)
                .title("챌린지1")
                .depositAmount(5000)
                .status(ChallengeStatus.RECRUITING)
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(3).plusWeeks(1))
                .createdAt(LocalDateTime.now())
                .build();

        em.persist(user1);
        em.persist(user2);
        em.persist(challenge);

        Participation p1 = Participation.createParticipation(user1, challenge);
        Participation p2 = Participation.createParticipation(user2, challenge);

        em.persist(p1);
        em.persist(p2);
        em.flush();
        em.clear(); // 1차 캐시 비우기

        // when
        List<Participation> results = participationRepository.findByChallenge_Id(challenge.getId());

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(p -> p.getChallenge().getId())
                           .containsOnly(challenge.getId());
    }
}