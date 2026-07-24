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

## 인증글 / 상호체크 시나리오

인증글은 **ch6 / ch7 에만** 들어 있다. 두 챌린지 모두 진행중(`ONGOING`)이라 오늘 등록이 가능하다.

| 챌린지 | 참여자 | `requiredChecks` (= 참여자수 - 1) | 오늘(2026-07-23) 인증글 |
|---|---|:--:|---|
| ch6 알고리즘 1일 1문제 | u2, u6, u7 | **2** | 3명 전원 등록됨 (v3/v5/v6) |
| ch7 저녁 금식 챌린지 | u3, u9 | **1** | **없음** — 정상 등록 재현용 |

**엔드포인트**

| 메서드 | 경로 |
|---|---|
| POST | `/api/verifications` — 등록 |
| PUT | `/api/verifications/{id}` — 수정 |
| GET | `/api/verifications/{id}` — 단건 |
| GET | `/api/verifications?challengeId=&participationId=&date=` — 목록 |
| POST | `/api/verifications/{id}/check` — 상호체크 |

응답의 `mine` / `checkedByMe` 로 화면이 수정 버튼 / 체크 버튼을 그린다.

| 확인할 것 | 요청 | 기대 |
|---|---|---|
| 정상 등록 | u3 로 `POST /api/verifications` `{challengeId:7, imageUrl:"..."}` | 200 |
| 하루 1회 | 같은 요청 재시도 | 409 |
| 미참여 | u5 로 ch7 인증글 | 403 |
| 기간 밖 | u2 로 ch1(`2026-08-01` 시작) 인증글 | 400 |
| `imageUrl` 누락 | `imageUrl` 없이 등록 | 400 |
| 미인증 | 토큰 없이 등록 | 401 |
| 정상 수정 | 방금 등록한 글을 u3 가 `PUT` (체크 0건) | 200, `imageUrl` 교체 |
| 남의 글 수정 | u2 가 u3 글 `PUT` | 403 |
| 체크받은 글 수정 | u2 가 v1(체크 2건) `PUT` | **409** — 체크 후 사진 교체 차단 |
| 수정 시 `imageUrl` 누락 | `PUT` body `{}` | 400 |
| 단건 조회 | `GET /api/verifications/1` | 200 |
| 챌린지 피드 | `GET ?challengeId=6` | 전체 기간 |
| **날짜별 피드** | `GET ?challengeId=6&date=2026-07-23` | 그날 인증글 (체크 누르는 화면) |
| 참여자 이력 | `GET ?participationId=9` | u2 의 ch6 인증 이력 |
| 필터 누락 | `GET /api/verifications` | 400 |
| 본인 글 자가 체크 | u2(작성자) 가 `POST /api/verifications/3/check` | 400 |
| 중복 체크 | u6 가 v3 체크 (이미 체크함) | 409 |
| 없는 인증글 | `/api/verifications/9999/check` | 404 |
| 외부인 체크 | u5(ch6 미참여) 가 v3 체크 | 403 |
| **마지막 체크 → 성공 판정** | u7 이 v3 체크 (1/2 → 2/2) | 200, `v3.succeeded=1` + `p9.success_count` 2→3 |

**플래그 확인용 시나리오** — u7 토큰으로 `GET ?challengeId=6&date=2026-07-23`

| 인증글 | 작성자 | `mine` | `checkedByMe` | 화면 |
|---|---|:--:|:--:|---|
| v6 | u7(본인) | `true` | `false` | 수정 버튼 |
| v5 | u6 | `false` | `false` | 체크 버튼 활성 |
| v3 | u2 | `false` | `false` → 체크 후 `true` | 체크 후 비활성 |

**참여 API** — `ParticipationServiceImpl` 은 **`RECRUITING` 상태만 참여를 허용**한다. 모집중인 ch1~ch5 를 쓸 것.

| 확인할 것 | 요청 | 기대 |
|---|---|---|
| 중복 참여 | u2 로 `POST /api/challenges/1/participations` | 409 |
| 잔액 부족 | u10(잔액 0) 로 ch1(보증금 10,000) 참여 | 400 |
| 충전 후 참여 | u10 `POST /api/points/charge` `{amount:50000}` → 재참여 | 200, `DEPOSIT_LOCK` 기록 |
| 없는 챌린지 | `POST /api/challenges/999/participations` | 404 |
| 모집중 아님 | u10 로 ch6(`ONGOING`) 참여 | **500** — 전용 예외가 없어 `RuntimeException` 을 던진다 (개선 과제) |
| 참여자 목록 | `GET /api/challenges/6/participations` | 200 |

> **`verified_date = '2026-07-23'` 행은 "오늘" 기준 데이터

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
UNION ALL SELECT 'challenge',          COUNT(*) FROM challenge
UNION ALL SELECT 'participation',      COUNT(*) FROM participation
UNION ALL SELECT 'verification',       COUNT(*) FROM verification
UNION ALL SELECT 'verification_check', COUNT(*) FROM verification_check
UNION ALL SELECT 'point_history',      COUNT(*) FROM point_history
UNION ALL SELECT 'refresh_token',      COUNT(*) FROM refresh_token;
-- 10 / 12 / 26 / 8 / 9 / 48 / 2
```

```sql
SELECT p.participation_id, p.success_count,
       (SELECT COUNT(*) FROM verification v
         WHERE v.participation_id = p.participation_id AND v.succeeded = 1) AS succeeded_rows
FROM participation p
WHERE p.participation_id IN (9, 10, 11, 12, 13);
```

---