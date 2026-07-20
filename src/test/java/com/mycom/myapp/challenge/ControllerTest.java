package com.mycom.myapp.challenge;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import lombok.extern.slf4j.Slf4j;

@WebMvcTest(ChallengeController.class)
@Slf4j
public class ControllerTest {

	@Autowired
	MockMvc mockMvc;
	
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
	
	
	
}
