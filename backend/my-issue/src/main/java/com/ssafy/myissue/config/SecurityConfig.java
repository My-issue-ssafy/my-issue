package com.ssafy.myissue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean @Order(1)
    SecurityFilterChain swagger(HttpSecurity http) throws Exception {
        http.securityMatcher("/swagger-ui/**","/v3/api-docs/**","/v3/api-docs.yaml")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(cs -> cs.disable())
                .httpBasic(h -> h.disable())
                .formLogin(f -> f.disable());
        return http.build();
    }

    @Bean @Order(2)
    SecurityFilterChain app(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(cs -> cs.disable());
        return http.build();
    }
}