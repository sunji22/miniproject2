package com.mycom.myapp.verification.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.mycom.myapp.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 상호체크 엔티티 - 이 인증글을 이 회원이 확인했다 1건
//  UNIQUE(verification_id, checker_user_id) = 1인 1체크
@Entity
@Table(name = "verification_check", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"verification_id", "checker_user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class VerificationCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_check_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id", nullable = false)
    private Verification verification;

    // 체크한 사람(= 같은 챌린지의 다른 참여자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checker_user_id", nullable = false)
    private User checker;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
