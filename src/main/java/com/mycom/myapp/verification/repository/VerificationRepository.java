package com.mycom.myapp.verification.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mycom.myapp.verification.entity.Verification;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, Long> {

    boolean existsByParticipation_IdAndVerifiedDate(Long participationId, LocalDate verifiedDate);

    // join fetch 를 건 이유:
    // VerificationResponse 를 만들 때 user.getName() / participation.getChallenge().getId() 를 읽는다.
    // LAZY 프록시 상태면 인증글 1건마다 초기화 쿼리가 나가서 N+1 이 된다.
    // 목록 쿼리 한 번에 필요한 연관을 다 끌고 온다.

    // 챌린지 피드 (전체 기간)
    @Query("SELECT v FROM Verification v "
            + "JOIN FETCH v.user "
            + "JOIN FETCH v.participation p "
            + "JOIN FETCH p.challenge c "
            + "WHERE c.id = :challengeId "
            + "ORDER BY v.verifiedDate DESC, v.createdAt DESC")
    List<Verification> findFeedByChallenge(@Param("challengeId") Long challengeId);

    // 챌린지 피드 (특정 날짜) - 그날 올라온 인증글을 모아 보고 체크를 누르는 화면
    @Query("SELECT v FROM Verification v "
            + "JOIN FETCH v.user "
            + "JOIN FETCH v.participation p "
            + "JOIN FETCH p.challenge c "
            + "WHERE c.id = :challengeId AND v.verifiedDate = :verifiedDate "
            + "ORDER BY v.createdAt DESC")
    List<Verification> findFeedByChallengeAndDate(@Param("challengeId") Long challengeId,
                                                  @Param("verifiedDate") LocalDate verifiedDate);

    // 특정 참여자의 인증 이력 (전체 기간)
    @Query("SELECT v FROM Verification v "
            + "JOIN FETCH v.user "
            + "JOIN FETCH v.participation p "
            + "JOIN FETCH p.challenge c "
            + "WHERE p.id = :participationId "
            + "ORDER BY v.verifiedDate DESC")
    List<Verification> findFeedByParticipation(@Param("participationId") Long participationId);

    // 특정 참여자의 특정 날짜 인증글
    @Query("SELECT v FROM Verification v "
            + "JOIN FETCH v.user "
            + "JOIN FETCH v.participation p "
            + "JOIN FETCH p.challenge c "
            + "WHERE p.id = :participationId AND v.verifiedDate = :verifiedDate")
    List<Verification> findFeedByParticipationAndDate(@Param("participationId") Long participationId,
                                                      @Param("verifiedDate") LocalDate verifiedDate);
}
