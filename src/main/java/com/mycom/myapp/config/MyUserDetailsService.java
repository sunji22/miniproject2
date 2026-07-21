package com.mycom.myapp.config;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

// 이메일로 회원을 조회해 Security 표준 UserDetails 로 변환하는 서비스
@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Security 는 파라미터명을 username 으로 부르지만, 본 프로젝트 로그인 식별자는 email
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(user.getRole().authority()));

            return MyUserDetails.builder()
                    .username(user.getEmail())   // Security 계약 필드(= email)
                    .password(user.getPassword())// Security 계약 필드(BCrypt 해시)
                    .authorities(authorities)    // Security 계약 필드
                    .id(user.getUserId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .build();
        }

        // 존재하지 않는 이메일 예외 처리
        throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
    }
}
