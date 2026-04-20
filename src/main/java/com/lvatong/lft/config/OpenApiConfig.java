package com.lvatong.lft.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lvatongOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("律法通API")
                .description("律法通APP - 智能法律咨询平台API文档")
                .version("v0.1")
                .contact(new Contact()
                    .name("律法通团队")));
    }
}
