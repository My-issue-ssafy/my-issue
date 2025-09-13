package com.ssafy.myissue.common.config;

ㅌimport com.ssafy.myissue.user.token.JwtAuthenticationFilter;
import com.ssafy.myissue.user.token.JwtIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtIssuer jwtIssuer;

    @Bean @Order(1)
    SecurityFilterChain swagger(HttpSecurity http) throws Exception {
        http.securityMatcher("/swagger-ui/**","/v3/api-docs/**","/v3/api-docs.yaml", "/v3/api-docs")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(cs -> cs.disable())
                .httpBasic(h -> h.disable())
                .formLogin(f -> f.disable());
        return http.build();
    }

    @Bean @Order(2)
    SecurityFilterChain app(HttpSecurity http, AuthenticationConfiguration authCfg, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(cs -> cs.disable())
                .sessionManagement(m -> m.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 공개 엔드포인트
                        .requestMatchers(HttpMethod.POST, "/auth/device", "/auth/reissue").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                .cors(c -> c.configurationSource(corsConfigurationSource))
                .httpBasic(h -> h.disable())
                .formLogin(f -> f.disable())
                .addFilterBefore(new JwtAuthenticationFilter(jwtIssuer), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://j13d101.p.ssafy.io"
        ));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        c.setExposedHeaders(List.of("Authorization", "X-Access-Token"));
        c.setAllowCredentials(true); // 쿠키(Refresh) 전송 허용

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}