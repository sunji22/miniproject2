package com.mycom.myapp.auth.service;

import com.mycom.myapp.auth.dto.LoginRequest;
import com.mycom.myapp.auth.dto.LoginResponse;

public interface LoginService {

    // 이메일/비밀번호로 인증하고 성공 시 access 토큰 + 유저정보를 반환
    LoginResponse login(LoginRequest req);
}
