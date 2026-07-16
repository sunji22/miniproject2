# miniproject2 - 보증금 챌린지

챌린지에 가상 보증금을 걸고, 인증 실패 시 보증금이 차감되어 성공자에게 분배되는 서비스

## 기술 스택
- Spring Boot (Gradle) / Java 21
- Spring Data JPA + MySQL
- Spring Security + JWT

## 시작하기
```bash
git clone https://github.com/sunji22/miniproject2.git
cd miniproject2
cp .env.example .env      # .env 는 git 에 안 올라감. 값 채우기(DB 비번, JWT 시크릿)
```
- `.env` 에 `DB_URL / DB_USERNAME / DB_PASSWORD / JWT_SECRET` 설정

---

# 팀 협업 컨벤션

## 작업 흐름 (하나의 기능/수정 = 이슈 1개)

**Issue → Branch → 환경 → 구현/푸시 → PR** 순서로 진행한다. 절대 `main` 에 직접 커밋하지 않는다.

### 1. 이슈 생성
- 기능/수정 단위로 GitHub **Issues → New Issue** 작성
- 제목에 무엇을 할지 명확히 (예: `로그인 API 구현`, `정산 트랜잭션 버그 수정`)
- 본문에 할 일 체크리스트 / 관련 화면 / 담당자 적기

### 2. 이슈에서 브랜치 생성
- 생성한 이슈 우측 **Development → Create a branch** 클릭
- 브랜치 이름을 아래 규칙으로 정한다 (이슈 번호 포함 권장)
  - 형식: `<type>/<이슈번호>-<간단설명>`
  - 예: `feat/12-login-api`, `fix/20-settle-rollback`
- "Checkout locally" 명령이 뜨면 그대로 로컬에서 실행해 브랜치 받기
```bash
git fetch origin
git checkout feat/12-login-api
```

### 3. 환경 만들기
- `.env` 준비(`.env.example` 복사), DB 스키마 실행
- 빌드 되는지 확인 후 작업 시작

### 4. 구현 및 푸시
- 자기 도메인 패키지 안에서만 작업(충돌 최소화)
- 커밋 메시지는 아래 규칙(`<type>: <내용>`)
- 자주 커밋 + 푸시
```bash
git add .
git commit -m "feat: 로그인 API 구현"
git push
```

### 5. PR 작성
- GitHub 에서 **Pull Request** 생성 (base: `main` ← compare: 내 브랜치)
- PR 본문에 `Closes #12` 넣으면 머지 시 이슈 자동 종료
- 팀원 1명 이상 리뷰 후 머지 (가능하면 Squash merge)
- 머지 후 브랜치 삭제, 로컬은 `git checkout main && git pull`

---

## 네이밍 규칙 (커밋, 브랜치 공통)

| type | 의미 | 예시 |
|------|------|------|
| `feat` | 새 기능 | `feat: 챌린지 개설 API` |
| `fix` | 버그 수정 | `fix: 중복 참여 검증 누락` |
| `refactor` | 기능 변화 없는 구조 개선 | `refactor: 서비스 계층 분리` |
| `docs` | 문서(README 등) | `docs: API 명세 추가` |
| `test` | 테스트 코드 | `test: 정산 단위 테스트` |
| `chore` | 설정/빌드/잡일 | `chore: JaCoCo 설정 추가` |
| `style` | 포맷/세미콜론 등(로직 X) | `style: 코드 포맷 정리` |

- **커밋**: `<type>: <한글 설명>` (예: `feat: JWT 로그인 구현`)
- **브랜치**: `<type>/<이슈번호>-<영문 요약>` (예: `feat/12-login-api`)