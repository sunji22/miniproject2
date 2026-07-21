package com.mycom.myapp.challenge;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.mycom.myapp.challenge.controller.ChallengeController;
import com.mycom.myapp.challenge.domain.ChallengeStatus;
import com.mycom.myapp.challenge.dto.ChallengeDto;
import com.mycom.myapp.challenge.dto.ChallengeSearchConditionDto;
import com.mycom.myapp.challenge.entity.Challenge;
import com.mycom.myapp.challenge.repository.ChallengeRepository;
import com.mycom.myapp.challenge.service.ChallengeService;
import com.mycom.myapp.common.ResultDto;
import com.mycom.myapp.common.exception.ChallengeNotFoundException;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@AutoConfigureMockMvc 
@Transactional
public class ChallengeFindTest {

	@Autowired
    private MockMvc mockMvc;
	
	@Autowired
	private ChallengeRepository challengeRepository;
	
	@Autowired
	private ChallengeService challengeService;
	
	@Autowired
	private ChallengeController controller;
	
	@Test
	void 목록조회_service_테스트() {
		log.info("목록 조회 테스트");
		ChallengeSearchConditionDto condition = new ChallengeSearchConditionDto();
		condition.setTitle("일찍 자기");
		condition.setStatus(ChallengeStatus.RECRUITING);
		
		ResultDto<List<ChallengeDto>> result = challengeService.listChallenge(condition);
		
		log.info(result.getData().toString());
		log.info("페이징 개수={}", result.getData().size());
		
		assertNotNull(challengeService);
		assertNotNull(result);
		assertEquals("success", result.getResult());
		assertEquals(10, result.getData().size());
	}
	
	@Test
	void 상세조회_service_테스트() {
		log.info("상세 조회 테스트");
		
		Challenge entity = challengeRepository.findById(41L).get();
		ResultDto<ChallengeDto> result = challengeService.detailChallenge(41L);
		
		log.info("id={}, 제목={}", entity.getId(), entity.getTitle());
		
		assertNotNull(entity);
		assertEquals(41L, entity.getId());
		assertNotNull(result.getData());
		assertEquals(41L, result.getData().getId());
		assertEquals("success", result.getResult());
		
	}
	
	@Test
	void NotFound_예외_테스트() {
	    // given
	    Long invalidChallengeId = -1L;

	    // when & then
	    ChallengeNotFoundException exception = assertThrows(ChallengeNotFoundException.class, () -> {
	        challengeService.detailChallenge(invalidChallengeId);
	    });

	    log.info(exception.getMessage());
	    // 추가 검증: 에러 메시지 정합성 확인
	    assertEquals("챌린지를 찾을 수 없습니다. id="+invalidChallengeId, exception.getMessage());
	}
	
	@Test
	@WithMockUser
	void 목록조회_Controller_테스트() throws Exception {
		
		// RECRUITING -> Enum 으로 DB 조회까지 잘 되었는지.
        this.mockMvc.perform(get("/api/challenges")
		                        .param("status", "RECRUITING")
		                        .contentType(MediaType.APPLICATION_JSON)
		                    )
                	.andExpect(status().isOk())
                .andExpect( jsonPath("$.result").value("success") )
				.andExpect( jsonPath("$.data").exists() )
                .andExpect( jsonPath("$.data[0].status").value("RECRUITING") )
                .andDo(print());
	}
	
	@Test
	@WithMockUser
	void 상세조회_Controller_테스트() throws Exception {
		// given
		Long id = 12L;
		
		this.mockMvc.perform(get("/api/challenges/{id}", id)
				                .contentType(MediaType.APPLICATION_JSON)
				            )
					.andExpect(status().isOk())
				.andExpect( jsonPath("$.result").value("success") )
				.andExpect( jsonPath("$.data.id").value(id) )
				.andDo(print());
	}
}
