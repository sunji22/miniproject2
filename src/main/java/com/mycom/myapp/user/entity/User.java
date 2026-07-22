package com.mycom.myapp.user.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회원 엔티티
@Entity
@Table(name = "user")
@Getter
@Setter
// JPA 는 리플렉션으로 객체를 만들 때 파라미터 없는 생성자가 반드시 필요 -> @NoArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    // unique=true 를 걸어두면 ddl-auto 가 유니크 제약을 생성/검증
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    // BCrypt 해시 문자열 저장(평문 저장 금지)
    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // 가상 포인트 지갑 잔액
    @Column(name = "point_balance", nullable = false)
    private int pointBalance;

    // 가입 시각
    // @CreationTimestamp = INSERT 시점 자동 주입
    // updatable=false 로 이후 고정
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
