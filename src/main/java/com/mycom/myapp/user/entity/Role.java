package com.mycom.myapp.user.entity;

// 회원 역할(권한) enum
public enum Role {

    USER,   // 일반 회원 (가입 기본값)
    ADMIN;  // 관리자 (정산 등 관리 API 접근)

    // Security 권한 문자열
    // ADMIN.authority() -> "ROLE_ADMIN"
    public String authority() {
        return "ROLE_" + name();
    }
}
