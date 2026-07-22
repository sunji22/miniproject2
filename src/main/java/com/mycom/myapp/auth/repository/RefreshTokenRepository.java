package com.mycom.myapp.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mycom.myapp.auth.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // user_id 로 저장된 refresh 1행 조회
    Optional<RefreshToken> findByUser_UserId(Long userId);
}
