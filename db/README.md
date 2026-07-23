# 스키마 & 테스트 데이터

| 파일 | 내용 |
|---|---|
| `schema.sql` | 전체 DDL (DROP + CREATE). JPA 엔티티와 대조 완료 |
| `data.sql` | 샘플 데이터 (회원 10 / 챌린지 12 / 참여 26 / 포인트 이력 48 / 리프레시 토큰 2) |

이 폴더는 `src/main/resources` **밖**이라 Spring 이 자동 실행하지 않는다.
DB 초기화는 항상 수동으로 한다.

---

## 실행

```bash
mysql -u <user> -p -e "CREATE DATABASE IF NOT EXISTS challenge DEFAULT CHARACTER SET utf8mb4;"
mysql -u <user> -p challenge < db/schema.sql
mysql -u <user> -p challenge < db/data.sql
```

`schema.sql` 은 맨 앞에서 6개 테이블을 **DROP** 한다. 기존 데이터가 날아가므로 개발 DB 에서만 실행할 것.

---

## 계정

전 계정 비밀번호: **`password123`** (BCrypt `$2y$10`)

| id | email | 이름 | role | 잔액 | 용도 |
|---|---|---|---|---|---|
| 1 | admin@challenge.com | 관리자 | ADMIN | 1,000,000 | `/api/admin/**`, 정산 API |
| 2 | hong@test.com | 홍길동 | USER | 140,000 | 호스트 겸 참여자, 이력 풍부 |
| 3 | kim@test.com | 김철수 | USER | 105,000 | 다중 챌린지 참여 |
| 4 | lee@test.com | 이영희 | USER | 137,000 | SUCCESS/FAILED 둘 다 보유 |
| 5 | park@test.com | 박민수 | USER | **0** | 잔액 부족 예외 테스트 |
| 6 | choi@test.com | 최지우 | USER | 94,000 | 진행중 다수 |
| 7 | jung@test.com | 정다은 | USER | 40,000 | 인증 미달 실패만 보유 |
| 8 | kang@test.com | 강호동 | USER | 255,000 | 정산 수혜자(REWARD) |
| 9 | yoon@test.com | 윤서연 | USER | 210,000 | 성공 참여자 |
| 10 | shin@test.com | 신동엽 | USER | 0 | **참여/이력 0건**, 빈 목록 조회 |

---

## 시나리오별 데이터 매핑

| 확인할 것 | 쓸 데이터 | 기대 |
|---|---|---|
| 로그인 / JWT 발급 | `hong@test.com` / `password123` | access + refresh 토큰 |
| 회원가입 이메일 중복 | `hong@test.com` 로 `POST /api/users` | `EmailAlreadyExistsException` |
| 챌린지 목록 페이징 | `GET /api/challenges?page=0&size=10` | 10건 / `page=1` → 2건 |
| status 필터 + 정렬 | `?status=RECRUITING` | 5건, `created_at` 오름차순 → id 1,2,3,4,5 |
| 〃 | `?status=ONGOING` | 4건, 순서 **9, 8, 6, 7** (id 순서와 일부러 다름) |
| 〃 | `?status=CLOSED` | 3건, 순서 **12, 10, 11** |
| 챌린지 상세 | `GET /api/challenges/1` | 정상 |
| 없는 챌린지 | `GET /api/challenges/999` | `ChallengeNotFoundException` |
| 중복 참여 방지 | u2 가 ch1 재참여 | `uk_participation` 위반 / `DuplicateParticipationException` |
| 잔액 부족 | u5(잔액 0) 로 `POST /api/points/lock` | `InsufficientPointException` |
| 포인트 이력 최신순 | u4 토큰으로 `GET /api/points/history` | 8건, `created_at` 내림차순 |
| 이력 없는 회원 | u10 토큰으로 `GET /api/points/history` | 빈 배열 |
| 이미 정산됨 | `POST /api/settlements/penalty-all` `{"challengeId":10}` | `SettlementAlreadyDoneException` |
| 정산 실행 | `POST /api/settlements/penalty-all` `{"challengeId":9}` | ch9 → CLOSED, u8·u9 PENALTY 기록 |
| 분배 계산 | ch11 결과 확인 | 몰수 40,000 ÷ 성공 2명 = 1인당 20,000 |
| 없는 회원 | `userId=999` 로 포인트 API | `UserNotFoundException` |
| 만료 refresh 토큰 | `refresh_token` id=1 (u2, 2026-07-20 만료) | 만료 행 존재 (서명 검증은 별개) |

**정산 실행 테스트는 ch9 하나뿐이다.** ch10/11/12 는 이미 `CLOSED` 라 예외 경로만 탄다.
ch9 로 한 번 정산하고 나면 다시 `CLOSED` 가 되므로, 반복하려면 `data.sql` 을 다시 넣어야 한다.

---

## 정합성 검증 쿼리

각 회원의 마지막 `balance_after` 와 `user.point_balance` 가 일치해야 한다. **0행이면 정상.**

```sql
SELECT u.user_id, u.point_balance, ph.balance_after
FROM user u
JOIN point_history ph ON ph.user_id = u.user_id
WHERE ph.created_at = (SELECT MAX(created_at) FROM point_history WHERE user_id = u.user_id)
  AND u.point_balance <> ph.balance_after;
```

건수 확인:

```sql
SELECT 'user' t, COUNT(*) c FROM user
UNION ALL SELECT 'challenge',     COUNT(*) FROM challenge
UNION ALL SELECT 'participation', COUNT(*) FROM participation
UNION ALL SELECT 'verification',  COUNT(*) FROM verification
UNION ALL SELECT 'point_history', COUNT(*) FROM point_history
UNION ALL SELECT 'refresh_token', COUNT(*) FROM refresh_token;
-- 10 / 12 / 26 / 0 / 48 / 2
```

---