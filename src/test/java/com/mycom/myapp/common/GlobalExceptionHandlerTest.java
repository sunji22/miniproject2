package com.mycom.myapp.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mycom.myapp.common.exception.ChallengeNotFoundException;
import com.mycom.myapp.common.exception.DuplicateParticipationException;
import com.mycom.myapp.common.exception.EmailAlreadyExistsException;
import com.mycom.myapp.common.exception.InsufficientPointException;
import com.mycom.myapp.common.exception.SettlementAlreadyDoneException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

// GlobalExceptionHandler 단위 테스트 / 올바른 상태코드나 응답으로 변환하나 테스트
// assertEquals 로 예외 -> 기대한 HTTP 상태코드 , 응답 필드 매핑을 검증
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private final MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/api/challenges/999");

    @Test
    @DisplayName("InsufficientPointException -> 400")
    void badRequest() {
        ResponseEntity<ErrorResponse> res =
                handler.handleInsufficientPoint(new InsufficientPointException(5000, 1000), request);

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertEquals(400, res.getBody().getStatus());
        assertEquals("포인트가 부족합니다. 필요=5000, 보유=1000", res.getBody().getMessage());
    }

    @Test
    @DisplayName("AccessDeniedException -> 403")
    void forbidden() {
        ResponseEntity<ErrorResponse> res =
                handler.handleAccessDenied(new AccessDeniedException("denied"), request);

        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
        assertEquals(403, res.getBody().getStatus());
    }

    @Test
    @DisplayName("ChallengeNotFoundException -> 404, 메시지/경로 포함")
    void notFound() {
        ResponseEntity<ErrorResponse> res =
                handler.handleNotFound(new ChallengeNotFoundException(999L), request);

        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
        assertEquals(404, res.getBody().getStatus());
        assertEquals("챌린지를 찾을 수 없습니다. id=999", res.getBody().getMessage());
        assertEquals("/api/challenges/999", res.getBody().getPath());
    }

    @Test
    @DisplayName("DuplicateParticipationException -> 409")
    void conflict() {
        ResponseEntity<ErrorResponse> res =
                handler.handleConflict(new DuplicateParticipationException(), request);

        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        assertEquals(409, res.getBody().getStatus());
    }

    @Test
    @DisplayName("SettlementAlreadyDoneException -> 409")
    void settled() {
        ResponseEntity<ErrorResponse> res =
                handler.handleConflict(new SettlementAlreadyDoneException(10L), request);

        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
    }

    @Test
    @DisplayName("EmailAlreadyExistsException -> 409")
    void emailConflict() {
        ResponseEntity<ErrorResponse> res =
                handler.handleConflict(new EmailAlreadyExistsException("a@test.com"), request);

        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        assertEquals("이미 사용 중인 이메일입니다. email=a@test.com", res.getBody().getMessage());
    }

    @Test
    @DisplayName("예상 못한 Exception -> 500")
    void serverError() {
        ResponseEntity<ErrorResponse> res =
                handler.handleException(new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertEquals(500, res.getBody().getStatus());
    }
}
