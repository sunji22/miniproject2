package com.mycom.myapp.common;

import lombok.Getter;

// 공통 성공 응답 래퍼 (실패는 ErrorResponse + 상태코드가 담당)
// 응답을 { "result": "success", "data": ... } 형태로 통일해서 프론트가 일관되게 파싱할 수 있도록
// 제네릭 <T> 로 어떤 데이터든 담을 수 있음 (예: ResultDto<Long>, ResultDto<List<XxxDto>>).
@Getter
public class ResultDto<T> {

    private final String result; // "success" 고정 (실패는 이 DTO 를 쓰지 않음)
    private final T data;        // 실제 응답 데이터

    private ResultDto(String result, T data) {
        this.result = result;
        this.data = data;
    }

    // 데이터가 있는 성공 응답
    public static <T> ResultDto<T> success(T data) {
        return new ResultDto<>("success", data);
    }

    // 데이터가 없는 성공 응답 (예: 삭제 성공)
    public static ResultDto<Void> success() {
        return new ResultDto<>("success", null);
    }
}