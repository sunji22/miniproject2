-- =============================================================
-- 테스트 더미 데이터
-- =============================================================

-- 1. 회원 (호스트 1명 + 참여자 3명)
INSERT INTO user (user_id, email, password, name, role, point_balance, created_at)
VALUES
(1, 'host@test.com',   '$2a$10$dummyhash1', '호스트',   'ROLE_USER', 50000, NOW()),
(2, 'user1@test.com',  '$2a$10$dummyhash2', '참여자1',  'ROLE_USER', 10000, NOW()),
(3, 'user2@test.com',  '$2a$10$dummyhash3', '참여자2',  'ROLE_USER', 10000, NOW()),
(4, 'user3@test.com',  '$2a$10$dummyhash4', '참여자3',  'ROLE_USER', 10000, NOW());

-- 2. 챌린지
-- 챌린지1: 종료 + 정산 대기 (정산 테스트용)
INSERT INTO challenge (challenge_id, host_id, title, description, deposit_amount, required_count, start_date, end_date, status, settlement_status, created_at)
VALUES
(1, 1, '챌린지1 - 정산대기', '3일 챌린지 (이미 종료)', 3000, 3, DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'ONGOING', 'PENDING', NOW());

-- 챌린지2: 진행중 + 정산 대기
INSERT INTO challenge (challenge_id, host_id, title, description, deposit_amount, required_count, start_date, end_date, status, settlement_status, created_at)
VALUES
(2, 1, '챌린지2 - 진행중', '아직 진행 중인 챌린지', 5000, 5, DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'ONGOING', 'PENDING', NOW());

-- 챌린지3: 정산 완료
INSERT INTO challenge (challenge_id, host_id, title, description, deposit_amount, required_count, start_date, end_date, status, settlement_status, created_at)
VALUES
(3, 1, '챌린지3 - 정산완료', '이미 정산된 챌린지', 2000, 2, DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'CLOSED', 'SETTLED', NOW());

-- 3. 참여
-- 챌린지1 참여자 (성공 2명, 실패 1명)
INSERT INTO participation (participation_id, challenge_id, user_id, success_count, status, created_at)
VALUES
(1, 1, 2, 3, 'SUCCESS', NOW()),   -- 참여자1: 성공 (3회 달성)
(2, 1, 3, 3, 'SUCCESS', NOW()),   -- 참여자2: 성공 (3회 달성)
(3, 1, 4, 1, 'FAILED', NOW());    -- 참여자3: 실패 (1회만)

-- 챌린지2 참여자 (진행중)
INSERT INTO participation (participation_id, challenge_id, user_id, success_count, status, created_at)
VALUES
(4, 2, 2, 2, 'JOINED', NOW()),    -- 참여자1: 진행중
(5, 2, 3, 3, 'JOINED', NOW());    -- 참여자2: 진행중

-- 챌린지3 참여자 (정산 완료)
INSERT INTO participation (participation_id, challenge_id, user_id, success_count, status, created_at)
VALUES
(6, 3, 2, 2, 'SUCCESS', NOW()),   -- 참여자1: 성공
(7, 3, 3, 0, 'FAILED', NOW());    -- 참여자2: 실패

-- 4. 포인트 이력 (기존 데이터)
INSERT INTO point_history (point_history_id, user_id, participation_id, amount, type, balance_after, created_at)
VALUES
(1, 1, NULL, 50000, 'CHARGE', 50000, NOW()),
(2, 2, NULL, 10000, 'CHARGE', 10000, NOW()),
(3, 3, NULL, 10000, 'CHARGE', 10000, NOW()),
(4, 4, NULL, 10000, 'CHARGE', 10000, NOW());
