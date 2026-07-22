package com.mycom.myapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

// Swagger / OpenAPI 설정
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "X-AUTH-TOKEN";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme apiKeyScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY) // 헤더에 토큰 값을 그대로 담는 apiKey 방식
                .in(SecurityScheme.In.HEADER)
                .name(SCHEME_NAME);               // 실제 요청 헤더 이름 = X-AUTH-TOKEN

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(SCHEME_NAME);

        return new OpenAPI()
                .info(new Info()
                        .title("보증금 챌린지 API")
                        .description("미니프로젝트2 - 보증금 챌린지 REST API 문서 (팀 공통 테스트용)")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(SCHEME_NAME, apiKeyScheme))
                .addSecurityItem(securityRequirement);
    }
}
