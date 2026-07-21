package com.mycom.myapp.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mycom.myapp.user.entity.User;

// 회원 리포지토리
// JpaRepository<User, Long> 상속만으로 기본 CRUD(save/findById/findAll/delete...)가 자동 제공
// <User, Long> = 엔티티 타입 User, PK 타입 Long(User.id)
// 아래 메서드들은 Spring Data JPA 의 "쿼리 메서드" - 메서드 이름을 파싱해 SELECT 쿼리를 자동 생성
public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인/인증 시 이메일로 회원 조회
    Optional<User> findByEmail(String email);

    // 회원가입 시 이메일 중복 여부만 확인
    boolean existsByEmail(String email);
}
