package com.mycom.myapp.config;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Builder;
import lombok.Getter;

// Spring Security 표준 사용자 객체(UserDetails) 구현
@Builder
@Getter
public class MyUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    // Spring Security 계약 필드
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    // 비즈니스 추가 필드
    private final Long id;
    private final String name;
    private final String email;
}
