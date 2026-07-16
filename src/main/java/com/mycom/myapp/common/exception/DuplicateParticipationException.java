package com.mycom.myapp.common.exception;

// 같은 회원이 같은 챌린지에 다시 참여하려 할 때 던지는 예외
// DB 의 UNIQUE(challenge_id, user_id) 제약과 대응. 서비스에서 선검사 후 이 예외를 던짐
public class DuplicateParticipationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateParticipationException() {
        super("이미 참여한 챌린지입니다.");
    }
}
