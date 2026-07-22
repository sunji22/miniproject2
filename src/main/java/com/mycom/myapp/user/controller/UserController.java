package com.mycom.myapp.user.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mycom.myapp.common.ResultDto;
import com.mycom.myapp.user.dto.UserCreateRequest;
import com.mycom.myapp.user.dto.UserResponse;
import com.mycom.myapp.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // @Valid 로 UserCreateRequest 검증(실패 시 400)
    @PostMapping("/api/users")
    public ResultDto<UserResponse> register(@Valid @RequestBody UserCreateRequest req) {
        UserResponse userResponse = userService.register(req);
        return ResultDto.success(userResponse);
    }
}