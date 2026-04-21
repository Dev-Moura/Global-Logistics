package com.michael.global_logistics_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. DESABILITA O CSRF para permitir que o Postman faça POST
                .csrf(csrf -> csrf.disable())

                // 2. CONFIGURA AS PERMISSÕES
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated() // Exige michael/logistica123
                )

                // 3. HABILITA O LOGIN BÁSICO (Para o Postman usar o Basic Auth)
                .httpBasic(Customizer.withDefaults());


        return http.build();
    }
}