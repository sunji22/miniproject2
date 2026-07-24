package com.mycom.myapp.verification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mycom.myapp.verification.entity.VerificationCheck;

@Repository
public interface VerificationCheckRepository extends JpaRepository<VerificationCheck, Long> {

    boolean existsByVerification_IdAndChecker_UserId(Long verificationId, Long checkerUserId);

    // 성공 판정용 - 이 인증글이 받은 체크 수
    long countByVerification_Id(Long verificationId);

    // 수정 가능 여부 판정용 - 이 인증글에 체크가 하나라도 달렸는지
    boolean existsByVerification_Id(Long verificationId);


    // [인증글id, 체크수] 쌍
    @Query("SELECT c.verification.id, COUNT(c) FROM VerificationCheck c "
            + "WHERE c.verification.id IN :verificationIds "
            + "GROUP BY c.verification.id")
    List<Object[]> countGroupedByVerificationIds(@Param("verificationIds") List<Long> verificationIds);

    // 내가 이미 체크한 인증글 id 목록 (프론트가 체크 버튼을 비활성화하는 데 사용)
    @Query("SELECT c.verification.id FROM VerificationCheck c "
            + "WHERE c.checker.userId = :checkerUserId AND c.verification.id IN :verificationIds")
    List<Long> findCheckedVerificationIds(@Param("checkerUserId") Long checkerUserId,
                                          @Param("verificationIds") List<Long> verificationIds);
}
