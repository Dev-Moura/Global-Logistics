package com.michael.global_logistics_api.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI globalLogisticsOpenApi() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        Contact contact = new Contact()
                .name("Global Logistics Team")
                .email("team@global-logistics.dev");

        Info info = new Info()
                .title("Global Logistics API")
                .version("1.0.0")
                .description("API para gestão de organizações, remessas, rastreamento e logística global.")
                .contact(contact);

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(BEARER_AUTH);

        return new OpenAPI()
                .info(info)
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme))
                .addSecurityItem(securityRequirement);
    }
}
