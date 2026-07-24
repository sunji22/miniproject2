package com.mycom.myapp.challenge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mycom.myapp.challenge.controller.ChallengeController;
import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.service.ChallengeService;
import com.mycom.myapp.challenge.service.ParticipationService;
import com.mycom.myapp.config.MyUserDetails;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ChallengeController.class)
@Slf4j
public class ControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper; // DTO -> JSON 변환기

	@MockitoBean
    private ChallengeService challengeService;

	// ChallengeController 가 ParticipationService 도 주입받으므로 목이 필요하다.
	// 없으면 @WebMvcTest 컨텍스트 로딩 자체가 실패한다.
	@MockitoBean
	private ParticipationService participationService;

	/*
	 * 컨트롤러가 @AuthenticationPrincipal 로 받은 principal 이
	 * MyUserDetails 인지 instanceof 로 검사하므로,
	 * @WithMockUser(= 스프링 기본 User 타입)를 쓰면 401 이 난다.
	 * 실제 principal 타입 그대로 주입해야 한다.
	 */
	private MyUserDetails loginUser() {
		return MyUserDetails.builder()
				.username("hong@test.com")
				.password("")
				.authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
				.id(2L)
				.name("홍길동")
				.email("hong@test.com")
				.build();
	}

	@Test
	void 목록조회_테스트() throws Exception {
		this.mockMvc.perform( get("/api/challenges").with(user(loginUser())) )
					.andExpect( status().isOk() );
	}

	@Test
	void 목록조회_응답_테스트() throws Exception {
		// 서비스는 raw data 를 반환하고, 컨트롤러가 ResultDto 로 감싼다
		List<ChallengeDto> data = List.of(new ChallengeDto());

	    Mockito.when(challengeService.listChallenge(any(ChallengeSearchConditionDto.class)))
	    		.thenReturn(data);

		this.mockMvc.perform( get("/api/challenges").param("status", "RECRUITING").with(user(loginUser())) )
					.andExpect( status().isOk() )
					.andExpect( content().contentType(MediaType.APPLICATION_JSON) )
					.andExpect( jsonPath("$.result").value("success") )
					.andExpect( jsonPath("$.data").exists() );
	}


	@Test
    void 등록_테스트() throws Exception {
        // ==========================================
        // [1] GIVEN: 요청 DTO 및 서비스 응답 Stubbing
        // ==========================================
        ChallengeDto requestDto = ChallengeDto.builder()
                .title("테스트 챌린지")
                .description("테스트 설명")
                .depositAmount(10000)
                .requiredCount(5)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .status(ChallengeStatus.RECRUITING)
                .build();

        // insertChallenge(dto, userId) 가 호출되면 1L 을 반환하도록 고정
        given(challengeService.insertChallenge(any(ChallengeDto.class), anyLong()))
                .willReturn(1L);

        // ==========================================
        // [2] WHEN & THEN: HTTP 요청 사출 및 결과 단언
        // ==========================================
        mockMvc.perform(
                    post("/api/challenges")
                        .with(user(loginUser()))                  // principal 을 MyUserDetails 로
                        .with(csrf()) // 1차 403 방어가드
                        .contentType(MediaType.APPLICATION_JSON) // JSON 포맷 명시
                        .content(objectMapper.writeValueAsString(requestDto)) // JSON 바디 실음
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("success"))
                .andExpect(jsonPath("$.data").value(1));
    }
}
