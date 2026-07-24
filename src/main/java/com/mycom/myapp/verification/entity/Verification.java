package com.mycom.myapp.verification.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.mycom.myapp.challenge.entity.Participation;
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

// 인증글 엔티티
// UNIQUE(participation_id, verified_date) = 하루 1인증
@Entity
@Table(name = "verification", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"participation_id", "verified_date"})
})
@Getter
@Setter
@NoArgsConstructor
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    private Participation participation;

    // 작성자
    // participation.user 와 같은 값이지만 내 글 조회/인가 편의를 위해 비정규화
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 업로드 인프라가 없어 URL 문자열만
    @Column(name = "image_url")
    private String imageUrl;

    @Column(length = 500)
    private String content;

    // 인증 날짜
    // 서버 기준 오늘로 고정 (과거/미래 날짜 조작 방지)
    @Column(name = "verified_date", nullable = false)
    private LocalDate verifiedDate;

    // 상호체크 정원을 채워 성공 판정이 끝났는지 여부
    @Column(nullable = false)
    private boolean succeeded = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
