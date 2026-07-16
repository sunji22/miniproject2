package com.mycom.myapp.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// (생성자가 메세지를 잘만드는가) 단위 테스트
class ChallengeExceptionsTest {

    @Test
    @DisplayName("ChallengeNotFound - id 가 포함된 메시지")
    void challengeNotFound() {
        ChallengeNotFoundException ex = assertThrows(
                ChallengeNotFoundException.class,
                () -> { throw new ChallengeNotFoundException(999L); });
        assertEquals("챌린지를 찾을 수 없습니다. id=999", ex.getMessage());
    }

    @Test
    @DisplayName("DuplicateParticipation - 고정 메시지")
    void duplicateParticipation() {
        DuplicateParticipationException ex = assertThrows(
                DuplicateParticipationException.class,
                () -> { throw new DuplicateParticipationException(); });
        assertEquals("이미 참여한 챌린지입니다.", ex.getMessage());
    }

    @Test
    @DisplayName("DuplicateVerification - 고정 메시지")
    void duplicateVerification() {
        DuplicateVerificationException ex = assertThrows(
                DuplicateVerificationException.class,
                () -> { throw new DuplicateVerificationException(); });
        assertEquals("오늘 이미 인증했습니다.", ex.getMessage());
    }

    @Test
    @DisplayName("InsufficientPoint - 필요/보유 금액 메시지")
    void insufficientPoint() {
        InsufficientPointException ex = assertThrows(
                InsufficientPointException.class,
                () -> { throw new InsufficientPointException(5000, 1000); });
        assertEquals("포인트가 부족합니다. 필요=5000, 보유=1000", ex.getMessage());
    }

    @Test
    @DisplayName("SettlementAlreadyDone - id 가 포함된 메시지")
    void settlementAlreadyDone() {
        SettlementAlreadyDoneException ex = assertThrows(
                SettlementAlreadyDoneException.class,
                () -> { throw new SettlementAlreadyDoneException(10L); });
        assertEquals("이미 정산된 챌린지입니다. id=10", ex.getMessage());
    }

    @Test
    @DisplayName("EmailAlreadyExists - email 이 포함된 메시지")
    void emailAlreadyExists() {
        EmailAlreadyExistsException ex = assertThrows(
                EmailAlreadyExistsException.class,
                () -> { throw new EmailAlreadyExistsException("a@test.com"); });
        assertEquals("이미 사용 중인 이메일입니다. email=a@test.com", ex.getMessage());
    }

    @Test
    @DisplayName("UserNotFound - id 가 포함된 메시지")
    void userNotFound() {
        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> { throw new UserNotFoundException(7L); });
        assertEquals("회원을 찾을 수 없습니다. id=7", ex.getMessage());
    }
}
