package com.ssafy.myissue.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;


@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("My-issue API")
                        .description("API 명세서")
                        .version("v1.0.0"));
    }
//    @Bean
//    public GroupedOpenApi newsGroup() {
//        return GroupedOpenApi.builder()
//                .group("news")
//                // 실제 컨트롤러 매핑에 맞춰서 조정
//                .pathsToMatch("/api/news/**", "/news/**")
//                .packagesToScan("com.ssafy.myissue.news.controller")
//                .build();
//    }
}


