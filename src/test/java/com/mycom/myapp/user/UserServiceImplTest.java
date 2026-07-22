package com.mycom.myapp.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mycom.myapp.common.exception.EmailAlreadyExistsException;
import com.mycom.myapp.user.dto.UserCreateRequest;
import com.mycom.myapp.user.dto.UserResponse;
import com.mycom.myapp.user.entity.Role;
import com.mycom.myapp.user.entity.User;
import com.mycom.myapp.user.repository.UserRepository;
import com.mycom.myapp.user.service.UserServiceImpl;

// UserServiceImpl.register 단위 테스트 (Mockito, 스프링/DB 없음).
// @Mock 으로 UserRepository/PasswordEncoder 를 가짜로 만들고, @InjectMocks 로 서비스에 주입하는 방식
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserCreateRequest request() {
        return UserCreateRequest.builder()
                .email("a@test.com")
                .password("password123")
                .name("홍길동")
                .build();
    }

    // 이미 있는 이메일이면 가입 거부
    // EmailAlreadyExistsException 발생 & save 미호출 -> 맞으면 통과
    @Test
    @DisplayName("중복 이메일 -> EmailAlreadyExistsException, save 호출 안 함")
    void register_duplicateEmail() {
        // given : existsByEmail 이 true (이미 가입됨)
        when(userRepository.existsByEmail("a@test.com")).thenReturn(true);

        // when / then : 예외 발생
        assertThrows(EmailAlreadyExistsException.class, () -> userService.register(request()));

        // 저장 로직까지 가지 않아야 함
        verify(userRepository, never()).save(any());
    }

    // 정상 가입 시 비번 인코딩 + 역할 USER + 저장
    // encode 호출 & 저장 엔티티 role==USER & 응답 email 일치 -> 맞으면 통과
    @Test
    @DisplayName("정상 가입 -> 비번 BCrypt 인코딩, role=USER, save 호출")
    void register_success() {
        // given : 중복 아님, 인코딩 결과 스텁, save 는 받은 엔티티를 그대로 반환
        when(userRepository.existsByEmail("a@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        UserResponse response = userService.register(request());

        // then : 저장된 엔티티를 캡처해 필드 검증
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("ENCODED", saved.getPassword());   // 평문 아닌 인코딩 값 저장
        assertEquals(Role.USER, saved.getRole());       // 서버가 USER 강제
        assertEquals("a@test.com", saved.getEmail());
        assertEquals("a@test.com", response.getEmail()); // 응답 매핑 확인
        verify(passwordEncoder).encode("password123");
    }

    // 인코딩된 비번은 응답에 노출 안 됨
    @Test
    @DisplayName("정상 가입 -> 응답 role 매핑 확인")
    void register_responseRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.register(request());

        assertEquals(Role.USER, response.getRole());
    }
}
