package com.mycom.myapp.challenge.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자의 검색 조건 매핑용 DTO
 * 	- 검색어
 * 	- 페이징 단위
 * 	- 상태(모집중, 진행중, 종료)
 */
@Getter
@Setter
@NoArgsConstructor
public class ChallengeSearchConditionDto {

	private String title;
    private String status;
    private int page = 0;
    private int size = 10;
}
