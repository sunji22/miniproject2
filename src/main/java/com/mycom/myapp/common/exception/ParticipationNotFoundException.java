package com.mycom.myapp.common.exception;

// 참여 정보가 없는 경우 예외 발생
public class ParticipationNotFoundException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

    public ParticipationNotFoundException(Long participationId) {
        super("참여 정보를 찾을 수 없습니다. id=" + participationId);
    }
    
    public ParticipationNotFoundException() {
		super("참여자가 없습니다.");
	}
}
