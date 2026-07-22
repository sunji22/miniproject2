package com.mycom.myapp.user.service;

import com.mycom.myapp.user.dto.UserCreateRequest;
import com.mycom.myapp.user.dto.UserResponse;

public interface UserService {

    UserResponse register(UserCreateRequest req);
}
