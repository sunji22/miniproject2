package com.mycom.myapp.auth.service;

import com.mycom.myapp.auth.dto.LoginRequest;
import com.mycom.myapp.auth.dto.LoginResponse;
import com.mycom.myapp.auth.dto.TokenRefreshRequest;
import com.mycom.myapp.auth.dto.TokenResponse;

public interface LoginService {

    // 이메일/비밀번호로 인증하고 성공 시 access + refresh 토큰 + 유저정보를 반환
    LoginResponse login(LoginRequest req);

    // refresh 토큰으로 access + refresh 를 재발급
    TokenResponse reissue(TokenRefreshRequest req);
}
