package com.mycom.myapp.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 공통 성공 응답 DTO(ResultDto) 단위 테스트 / 순수 JUnit 테스트
// assertEquals / assertNotNull / assertNull 로 기대값 vs 실제값 검증
class ResultDtoTest {

    @Test
    @DisplayName("success(data) - result=success, data 가 담긴다")
    void success_withData() {
        // when : 데이터를 담아 성공 응답 생성
        ResultDto<String> dto = ResultDto.success("hello");

        // then : result 는 success, data 는 넣은 값
        assertEquals("success", dto.getResult());
        assertNotNull(dto.getData());
        assertEquals("hello", dto.getData());
    }

    @Test
    @DisplayName("success() - data 는 null (예: 삭제 성공)")
    void success_noData() {
        ResultDto<Void> dto = ResultDto.success();

        assertEquals("success", dto.getResult());
        assertNull(dto.getData());
    }
}
