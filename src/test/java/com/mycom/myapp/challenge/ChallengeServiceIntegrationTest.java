package com.mycom.myapp.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.domain.SettlementStatus;
import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.entity.Participation;
import com.mycom.myapp.challenge.entity.ParticipationStatus;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.repository.ParticipationRepository;
import com.mycom.myapp.challenge.service.ChallengeService;
import com.mycom.myapp.common.exception.CannotDeleteOngoingChallengeException;
import com.mycom.myapp.user.entity.Role;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class ChallengeServiceIntegrationTest {

    @Autowired
    private ChallengeService challengeService;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParticipationRepository participationRepository;
    
    @Autowired
    private EntityManager em; // 🎯 EntityManager 주입 추가

    private User hostUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("tmdwl7978@naver.com");
        user.setPassword("qwer");
        user.setName("이승지");
        user.setRole(Role.USER);
        user.setPointBalance(500000);
        
        hostUser = userRepository.save(user);
        
    	// 🎯 기존 데이터로 인한 간섭 제거
        participationRepository.deleteAll(); // 연관 자식 엔티티 먼저 삭제 (FK 제약조건 방지)
        challengeRepository.deleteAll();
    }

    // ================= [ 챌린지 생성 테스트 ] =================

    @Test
    @DisplayName("챌린지 생성 통합 테스트")
    // [검증 요약] 챌린지 DB 저장 영속화 및 개설자의 자동 참여(Participation) 레코드 생성 연동을 검증합니다.
    void insertChallenge_Integration_Success() {
        // given
        ChallengeDto dto = ChallengeDto.builder()
                .title("통합테스트 챌린지")
                .description("상세 설명")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .requiredCount(5)
                .depositAmount(5000)
                .createdAt(LocalDateTime.now())
                .build();

        // when
        Long savedChallengeId = challengeService.insertChallenge(dto, hostUser.getUserId());

        // then
        Optional<Challenge> foundChallenge = challengeRepository.findById(savedChallengeId);
        assertThat(foundChallenge).isPresent();
        assertThat(foundChallenge.get().getTitle()).isEqualTo("통합테스트 챌린지");

        Optional<Participation> participation = participationRepository
                .findByChallenge_IdAndUser_UserIdAndStatus(savedChallengeId, hostUser.getUserId(), ParticipationStatus.JOINED);
        assertThat(participation).isPresent();
    }

    // ================= [ 챌린지 조회 테스트 ] =================

    @Test
    @DisplayName("챌린지 목록 조회 통합 테스트")
    // [검증 요약] 상태 검색 조건(RECRUITING)과 페이징 처리에 따른 정확한 챌린지 목록 조회를 검증합니다.
    void listChallenge_Integration_Success() {
        // given
        Challenge recruitingChallenge = Challenge.builder()
                .title("모집 중 챌린지")
                .status(ChallengeStatus.RECRUITING)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .createdAt(LocalDateTime.now())
                .host(hostUser)
                .build();
        Challenge ongoingChallenge = Challenge.builder()
                .title("진행 중 챌린지")
                .status(ChallengeStatus.ONGOING)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .createdAt(LocalDateTime.now())
                .host(hostUser)
                .build();
        challengeRepository.save(recruitingChallenge);
        challengeRepository.save(ongoingChallenge);

        ChallengeSearchConditionDto condition = new ChallengeSearchConditionDto();
        condition.setPage(0);
        condition.setSize(10);
        condition.setStatus(ChallengeStatus.RECRUITING);

        // when
        List<ChallengeDto> resultList = challengeService.listChallenge(condition);

        // then
        assertThat(resultList).hasSize(1);
        assertThat(resultList.get(0).getTitle()).isEqualTo("모집 중 챌린지");
    }
    
    @Test
    @DisplayName("챌린지 목록 페이징 검증 - 페이지 번호와 크기에 따라 데이터가 올바르게 분할 조회된다.")
    void listChallenge_Pagination_Success() {
        // given: 총 15개의 챌린지 저장
        for (int i = 1; i <= 15; i++) {
            Challenge challenge = Challenge.builder()
                    .title("테스트 챌린지 " + i)
                    .description("테스트 설명 " + i)
                    .depositAmount(1000)
                    .requiredCount(5)
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(10))
                    .status(ChallengeStatus.RECRUITING)
                    .settlementStatus(SettlementStatus.PENDING)
                    .createdAt(LocalDateTime.now()) // 🎯 필수: NOT NULL 제약조건 해제
                    .host(hostUser)
                    .build();
            challengeRepository.save(challenge);
        }

        // Page 0 (size 10) 조건 설정
        ChallengeSearchConditionDto page0Condition = new ChallengeSearchConditionDto();
        page0Condition.setPage(0);
        page0Condition.setSize(10);
        page0Condition.setStatus(ChallengeStatus.RECRUITING);

        // Page 1 (size 10) 조건 설정
        ChallengeSearchConditionDto page1Condition = new ChallengeSearchConditionDto();
        page1Condition.setPage(1);
        page1Condition.setSize(10);
        page1Condition.setStatus(ChallengeStatus.RECRUITING);

        // when
        List<ChallengeDto> page0Result = challengeService.listChallenge(page0Condition);
        List<ChallengeDto> page1Result = challengeService.listChallenge(page1Condition);

        // then
        assertThat(page0Result).hasSize(10); // 1번째 페이지 10개 검증
        assertThat(page1Result).hasSize(5);  // 2번째 페이지 남은 5개 검증
        assertThat(page0Result.get(0).getTitle()).isNotEqualTo(page1Result.get(0).getTitle()); // 데이터 중복 없음 검증
    }

    @Test
    @DisplayName("챌린지 상세 조회 통합 테스트")
    // [검증 요약] 인증된 사용자가 상세 조회 시 본인의 챌린지 참여 ID(participationId)가 올바르게 세팅되는지 검증합니다.
    void detailChallenge_Integration_Success() {
        // given
        Challenge challenge = Challenge.builder()
                .title("상세 조회 챌린지")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .createdAt(LocalDateTime.now())
                .status(ChallengeStatus.RECRUITING)
                .host(hostUser)
                .build();
        Challenge savedChallenge = challengeRepository.save(challenge);

        Participation participation = Participation.createParticipation(hostUser, savedChallenge);
        Participation savedParticipation = participationRepository.save(participation);

        // when
        ChallengeDto detailDto = challengeService.detailChallenge(savedChallenge.getId(), hostUser.getUserId());

        // then
        assertThat(detailDto.getTitle()).isEqualTo("상세 조회 챌린지");
        assertThat(detailDto.getParticipationId()).isEqualTo(savedParticipation.getId());
    }

    // ================= [ 챌린지 수정 테스트 ] =================

    @Test
    @DisplayName("챌린지 수정 통합 테스트")
    // [검증 요약] 챌린지 수정 시 JPA Dirty Checking에 의해 DB 레코드가 정상 업데이트되는지 검증합니다.
    void updateChallenge_Integration_Success() {
        // given
        Challenge challenge = Challenge.builder()
                .title("원래 제목")
                .description("원래 설명")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .createdAt(LocalDateTime.now())
                .requiredCount(3)
                .depositAmount(3000)
                .status(ChallengeStatus.RECRUITING)
                .host(hostUser)
                .build();
        Challenge saved = challengeRepository.save(challenge);

        ChallengeDto updateDto = ChallengeDto.builder()
                .id(saved.getId())
                .title("수정된 제목")
                .description("수정된 설명")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .createdAt(LocalDateTime.now())
                .requiredCount(3)
                .depositAmount(3000)
                .build();

        // when
        challengeService.updateChallenge(updateDto, hostUser.getUserId());

        // then
        Challenge updatedChallenge = challengeRepository.findById(saved.getId()).get();
        assertThat(updatedChallenge.getTitle()).isEqualTo("수정된 제목");
        assertThat(updatedChallenge.getDescription()).isEqualTo("수정된 설명");
    }

    // ================= [ 챌린지 삭제 테스트 ] =================

    @Test
    @DisplayName("진행 중인 챌린지 삭제 예외 통합 테스트")
    // [검증 요약] 진행 중(ONGOING)인 챌린지 삭제 시 예외가 발생하고 DB 레코드는 삭제되지 않고 보존되는지 검증합니다.
    void deleteChallenge_Integration_OngoingException() {
        // given
        Challenge ongoingChallenge = Challenge.builder()
                .title("진행 중 챌린지")
                .description("설명")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .createdAt(LocalDateTime.now())
                .requiredCount(3)
                .depositAmount(1000)
                .status(ChallengeStatus.ONGOING)
                .host(hostUser)
                .build();
        Challenge saved = challengeRepository.save(ongoingChallenge);

        // when & then
        assertThatThrownBy(() -> challengeService.deleteChallenge(saved.getId(), hostUser.getUserId()))
                .isInstanceOf(CannotDeleteOngoingChallengeException.class);

        assertThat(challengeRepository.existsById(saved.getId())).isTrue();
    }
    
    @Test
    @DisplayName("모집 중인 챌린지 정상 삭제 통합 테스트")
    // [검증 요약] 모집 중(RECRUITING)인 챌린지 삭제 시 DB 레코드가 실제로 제거(existsById == false)되는지 검증합니다.
    // 이건 참여자가 없는 챌린지. 참여 테이블에 FK 제약이 없기때문에 정상 삭제
    void deleteChallenge_Integration_Success() {
        // given: 삭제 가능한 모집 중(RECRUITING) 상태의 챌린지 생성
        Challenge recruitingChallenge = Challenge.builder()
                .title("삭제할 챌린지")
                .description("설명")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .createdAt(LocalDateTime.now())
                .requiredCount(3)
                .depositAmount(1000)
                .status(ChallengeStatus.RECRUITING)
                .host(hostUser)
                .build();
        Challenge saved = challengeRepository.save(recruitingChallenge);

        // when: 정상 삭제 수행
        challengeService.deleteChallenge(saved.getId(), hostUser.getUserId());

        // then: DB에서 실제로 삭제되었는지 검증 (existsById -> false)
        assertThat(challengeRepository.existsById(saved.getId())).isFalse();
    }
    
    @Test
    @DisplayName("참여자가 존재할 때 챌린지 삭제 시 DB 외래키 제약조건 위반 예외 발생")
    void deleteChallenge_Integration_Fail_WhenParticipationExists() {
        // given: 챌린지 생성 (insertChallenge 내부에서 개설자 자가 참여 처리까지 완료되어 Participation 연관 데이터 존재)
        Challenge recruitingChallenge = Challenge.builder()
                .title("삭제할 챌린지")
                .description("설명")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .createdAt(LocalDateTime.now())
                .requiredCount(3)
                .depositAmount(1000)
                .status(ChallengeStatus.RECRUITING)
                .host(hostUser)
                .build();

        Long challengeId = challengeService.insertChallenge(
                ChallengeDto.from(recruitingChallenge), hostUser.getUserId());

        // when & then: FK 참조 중인 Participation 이 존재하므로 삭제 시 DB 예외 발생 검증
        // DB 수준의 FK 제약조건 예외가 터지기 전에, JPA 영속성 컨텍스트가 객체 연관관계 정합성을 검사하여 메모리 단에서 먼저 예외를 던진다.
        assertThatThrownBy(() -> {
        	challengeService.deleteChallenge(challengeId, hostUser.getUserId());
        	challengeRepository.flush(); // 🎯 지연 쓰기 강제 수동 플러시
        }).isInstanceOf(InvalidDataAccessApiUsageException.class);

        em.clear(); // flush()로 오염된 1차 캐시 초기화
        
        // 예외 발생 후 DB에 챌린지 레코드가 롤백되어 안전하게 보존되었는지 확인
        assertThat(challengeRepository.existsById(challengeId)).isTrue();
    }
}