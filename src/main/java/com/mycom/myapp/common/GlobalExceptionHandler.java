package com.mycom.myapp.common;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mycom.myapp.common.exception.ChallengeNotFoundException;
import com.mycom.myapp.common.exception.DuplicateParticipationException;
import com.mycom.myapp.common.exception.DuplicateVerificationException;
import com.mycom.myapp.common.exception.EmailAlreadyExistsException;
import com.mycom.myapp.common.exception.InsufficientPointException;
import com.mycom.myapp.common.exception.SettlementAlreadyDoneException;

import jakarta.servlet.http.HttpServletRequest;

// 프로젝트 전체(전역) 예외 처리자
// - Controller/Service 에서 예외를 잡지 않고 그대로 위로 던지면, 여기(@RestControllerAdvice)가 받아
//   일관된 ErrorResponse + HTTP 상태코드로 변환
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 BAD_REQUEST : @Valid 입력 검증 실패
    // @Valid @RequestBody 검증에서 실패하면 MethodArgumentNotValidException 이 자동 발생
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // 필드 검증 메시지 중 첫 번째를 대표 메시지로 사용 (없으면 기본 문구)
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "입력값 검증에 실패했습니다.";

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // 400 BAD_REQUEST : 포인트 부족 (참여 시 잔액 < 보증금)
    @ExceptionHandler(InsufficientPointException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPoint(
            InsufficientPointException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // 403 FORBIDDEN : 인가 실패 (권한 없음)
    //   메서드 보안(@PreAuthorize 등)에서 올라온 AccessDeniedException 을 여기서 응답으로 변환.
    //   (SecurityFilterChain 단계의 인가 실패는 AccessDeniedHandler 가 처리하므로 여기 안 옴)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("접근 권한이 없습니다.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // 404 NOT_FOUND : 챌린지 없음
    //   (참여/인증글 등 다른 도메인 not-found 예외도 각 담당이 같은 패턴으로 추가 가능)
    @ExceptionHandler(ChallengeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ChallengeNotFoundException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // 409 CONFLICT : 상태 충돌 (중복/재실행)
    //   중복 참여, 중복 인증(하루 1회), 정산 재실행, 회원가입 이메일 중복 => 모두 409 로 통일
    @ExceptionHandler({
            DuplicateParticipationException.class,
            DuplicateVerificationException.class,
            SettlementAlreadyDoneException.class,
            EmailAlreadyExistsException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(
            RuntimeException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    // 500 INTERNAL_SERVER_ERROR : 예상하지 못한 모든 예외
    //   위에서 매핑되지 않은 예외(NullPointerException 포함)를 잡아 일관된 포맷으로 응답.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("서버 내부 오류가 발생했습니다.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
