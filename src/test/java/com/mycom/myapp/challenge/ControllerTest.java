package com.mycom.myapp.challenge;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mycom.myapp.challenge.controller.ChallengeController;
import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.service.ChallengeService;
import com.mycom.myapp.common.ResultDto;
import static org.mockito.BDDMockito.given;

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
	
	
	@Test
	@WithMockUser
	void 목록조회_테스트() throws Exception {
		this.mockMvc.perform( get("/api/challenges") )
					.andExpect( status().isOk() );
	}

	@Test
	@WithMockUser
	void 목록조회_응답_테스트() throws Exception {
		List<ChallengeDto> data = List.of(new ChallengeDto()); // 빈 data
	    ResultDto<List<ChallengeDto>> mockResult = ResultDto.success(data);
	    
	    Mockito.when(challengeService.listChallenge(any(ChallengeSearchConditionDto.class)))
	    		.thenReturn(mockResult);
	    
		this.mockMvc.perform( get("/api/challenges").param("status", "RECRUITING") )
					.andExpect( status().isOk() )
					.andExpect( content().contentType(MediaType.APPLICATION_JSON) )
					.andExpect( jsonPath("$.result").value("success") )
					.andExpect( jsonPath("$.data").exists() );
	}
	
	
	@Test
    @WithMockUser
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

        // service.insertChallenge가 호출되면 ResultDto.success(1L)을 반환하도록 고정
        given(challengeService.insertChallenge(any(ChallengeDto.class)))
                .willReturn(ResultDto.success(1L));

        // ==========================================
        // [2] WHEN & THEN: HTTP 요청 사출 및 결과 단언
        // ==========================================
        mockMvc.perform(
                    post("/api/challenges")
                        .with(csrf()) // 1차 403 방어가드
                        .contentType(MediaType.APPLICATION_JSON) // JSON 포맷 명시
                        .content(objectMapper.writeValueAsString(requestDto)) // JSON 바디 실음
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("success"))
                .andExpect(jsonPath("$.data").value(1));
    }
}
