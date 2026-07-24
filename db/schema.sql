-- 보증금 챌린지 - DB 스키마

DROP TABLE IF EXISTS point_history;
DROP TABLE IF EXISTS verification_check;
DROP TABLE IF EXISTS verification;
DROP TABLE IF EXISTS refresh_token;
DROP TABLE IF EXISTS participation;
DROP TABLE IF EXISTS challenge;
DROP TABLE IF EXISTS user;

-- ------------------------------------------------------------
-- 1. 회원  (com.mycom.myapp.user.entity.User)
-- ------------------------------------------------------------
CREATE TABLE user (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(100) NOT NULL,                -- 로그인 식별자 (findByEmail)
    password      VARCHAR(255) NOT NULL,                -- BCrypt 해시
    name          VARCHAR(50),
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER', -- enum Role: USER / ADMIN (Security 권한은 ROLE_ 접두어 부착)
    point_balance INT          NOT NULL DEFAULT 0,      -- 지갑 잔액(가상 포인트)
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    CONSTRAINT uk_user_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 2. 챌린지  (com.mycom.myapp.challenge.entity.Challenge)
-- ------------------------------------------------------------
CREATE TABLE challenge (
    challenge_id   BIGINT       NOT NULL AUTO_INCREMENT,
    host_id        BIGINT       NOT NULL,               -- 개설자 user(user_id)
    title          VARCHAR(255) NOT NULL,
    description    VARCHAR(255),
    deposit_amount INT          NOT NULL,               -- 참여 보증금
    required_count INT          NOT NULL,               -- 성공 판정 최소 인증 횟수
    start_date     DATE         NOT NULL,
    end_date       DATE         NOT NULL,
    status         VARCHAR(255) NOT NULL DEFAULT 'RECRUITING', -- enum ChallengeStatus: RECRUITING/ONGOING/CLOSED
    settlement_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- enum SettlementStatus: PENDING/SETTLED (정산 스케줄러가 사용)
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (challenge_id),
    CONSTRAINT fk_challenge_host FOREIGN KEY (host_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 3. 참여  (com.mycom.myapp.challenge.entity.Participation)
-- ------------------------------------------------------------
CREATE TABLE participation (
    participation_id BIGINT      NOT NULL AUTO_INCREMENT,
    challenge_id     BIGINT      NOT NULL,
    user_id          BIGINT      NOT NULL,
    success_count    INT         NOT NULL DEFAULT 0,
    status           VARCHAR(20) NOT NULL DEFAULT 'JOINED', -- enum ParticipationStatus: JOINED/SUCCESS/FAILED
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (participation_id),
    CONSTRAINT uk_participation UNIQUE (challenge_id, user_id),   -- 중복 참여 방지
    CONSTRAINT fk_participation_challenge FOREIGN KEY (challenge_id) REFERENCES challenge (challenge_id),
    CONSTRAINT fk_participation_user      FOREIGN KEY (user_id)      REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 4. 인증글  (com.mycom.myapp.verification.entity.Verification)
-- ------------------------------------------------------------
CREATE TABLE verification (
    verification_id  BIGINT       NOT NULL AUTO_INCREMENT,
    participation_id BIGINT       NOT NULL,
    user_id          BIGINT       NOT NULL,          -- 작성자 (participation.user_id 와 동일값, 인가/내글조회 편의 비정규화)
    image_url        VARCHAR(255),
    content          VARCHAR(500),
    verified_date    DATE         NOT NULL,
    succeeded        BIT(1)       NOT NULL DEFAULT 0, -- 상호체크 정원을 채워 성공 판정이 끝났는지 (successCount 1회 반영 가드)
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (verification_id),
    CONSTRAINT uk_verification UNIQUE (participation_id, verified_date), -- 하루 1인증
    CONSTRAINT fk_verification_participation FOREIGN KEY (participation_id) REFERENCES participation (participation_id),
    CONSTRAINT fk_verification_user          FOREIGN KEY (user_id)          REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 4-1. 상호체크  (com.mycom.myapp.verification.entity.VerificationCheck)
--   "이 인증글을 이 회원이 확인했다" 1건.
--   성공 판정: countByVerification_Id >= (챌린지 참여자수 - 1)
-- ------------------------------------------------------------
CREATE TABLE verification_check (
    verification_check_id BIGINT      NOT NULL AUTO_INCREMENT,
    verification_id       BIGINT      NOT NULL,
    checker_user_id       BIGINT      NOT NULL,      -- 체크한 사람 (= 같은 챌린지의 다른 참여자)
    created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (verification_check_id),
    CONSTRAINT uk_verification_check UNIQUE (verification_id, checker_user_id), -- 1인 1체크
    CONSTRAINT fk_check_verification FOREIGN KEY (verification_id) REFERENCES verification (verification_id),
    CONSTRAINT fk_check_user         FOREIGN KEY (checker_user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 5. 포인트 이력  (com.mycom.myapp.point.entity.PointHistory)
-- ------------------------------------------------------------
CREATE TABLE point_history (
    point_history_id BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,              -- 대상 회원
    participation_id BIGINT,                             -- 관련 참여 (CHARGE/WITHDRAW 는 NULL)
    amount           INT          NOT NULL,              -- 거래 금액 (아래 주의 참고)
    type             VARCHAR(255) NOT NULL,              -- enum PointType: CHARGE/DEPOSIT_LOCK/DEPOSIT_REFUND/PENALTY/REWARD/WITHDRAW
    balance_after    INT          NOT NULL,              -- 처리 후 잔액(정합성 추적)
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (point_history_id),
    CONSTRAINT fk_point_user          FOREIGN KEY (user_id)          REFERENCES user (user_id),
    CONSTRAINT fk_point_participation FOREIGN KEY (participation_id) REFERENCES participation (participation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 6. 리프레시 토큰  (com.mycom.myapp.auth.entity.RefreshToken)
-- ------------------------------------------------------------
CREATE TABLE refresh_token (
    refresh_token_id BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    token            VARCHAR(512) NOT NULL,          -- Refresh JWT 평문
    expires_at       DATETIME(6)  NOT NULL,          -- 만료 시각
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (refresh_token_id),
    CONSTRAINT uk_refresh_user UNIQUE (user_id),                                       -- 1:1
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
