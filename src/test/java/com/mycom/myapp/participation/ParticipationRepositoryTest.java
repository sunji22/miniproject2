package com.mycom.myapp.participation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.user.entity.Role;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@DataJpaTest // JPA 관련 Bean만 로딩하여 인메모리 DB(H2 등) 기반으로 빠르게 테스트
//🎯 실제 DB 설정(application.yml) 그대로 사용
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) 
@Slf4j
class ParticipationRepositoryTest {

    @Autowired
    private ParticipationRepository participationRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private TestEntityManager em; // 테스트 전용 EntityManager

    @Test
    void 데이터_생성_테스트() {
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

        // 검증
        assertThat(participationRepository.count()).isEqualTo(50);
    }
    
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
        results.forEach(p -> p.getChallenge().getTitle()); // N+1 select 발생?

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

        // when
        List<Participation> results = participationRepository.findByChallenge_Id(challenge.getId());
        results.forEach(p -> p.getUser().getName()); // N+1 select 발생?
        // (확인) Hibernate: select p1_0.participation_id,p1_0.challenge_id,p1_0.created_at,p1_0.status,p1_0.success_count,p1_0.user_id,u1_0.user_id,u1_0.created_at,u1_0.email,u1_0.name,u1_0.password,u1_0.point_balance,u1_0.role from participation p1_0 join user u1_0 on u1_0.user_id=p1_0.user_id where p1_0.challenge_id=?

        
        // then
        assertThat(results).hasSize(50);
        assertThat(results).extracting(p -> p.getChallenge().getId())
                           .containsOnly(challenge.getId());
    }
}