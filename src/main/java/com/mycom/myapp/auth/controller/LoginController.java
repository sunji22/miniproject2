package com.mycom.myapp.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mycom.myapp.auth.dto.LoginRequest;
import com.mycom.myapp.auth.dto.LoginResponse;
import com.mycom.myapp.auth.dto.TokenRefreshRequest;
import com.mycom.myapp.auth.dto.TokenResponse;
import com.mycom.myapp.auth.service.LoginService;
import com.mycom.myapp.common.ResultDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/api/auth/login")
    public ResultDto<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse loginResponse = loginService.login(req);
        return ResultDto.success(loginResponse);
    }

    // access 만료 시 refresh 로 재발급
    @PostMapping("/api/auth/refresh")
    public ResultDto<TokenResponse> reissue(@Valid @RequestBody TokenRefreshRequest req) {
        TokenResponse tokenResponse = loginService.reissue(req);
        return ResultDto.success(tokenResponse);
    }
}
