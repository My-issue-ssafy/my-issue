package com.ssafy.myissue.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@OpenAPIDefinition(
        security = @SecurityRequirement(name = "bearerAuth") // 전역 security 스키마 적용
)
@SecurityScheme(
        name = "bearerAuth",          // 스키마 이름 (Swagger 내 Reference용)
        type = SecuritySchemeType.HTTP, // HTTP 기반 인증 연결
        scheme = "bearer",            // Bearer 토큰 인증임을 명시
        bearerFormat = "JWT",         // 토큰 포맷(jwt) 명시
        in = SecuritySchemeIn.HEADER  // Authorization 헤더를 통해 전송됨
)
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("My-issue API")
                        .description("API 명세서")
                        .version("v1.0.0"));
    }
}


