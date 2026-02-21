package com.wedding.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI weddingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wedding Interactive System API")
                        .description("婚禮互動系統後端 API — 留言牆、互動投票、幸運抽獎、媒體方案管理")
                        .version("1.0.0"));
    }
}
