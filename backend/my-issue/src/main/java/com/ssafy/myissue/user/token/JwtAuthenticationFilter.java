package com.ssafy.myissue.user.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtIssuer jwtIssuer;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    private final RequestMatcher skipMatcher = new OrRequestMatcher(
            new AntPathRequestMatcher("/auth/device", "POST"),
            new AntPathRequestMatcher("/auth/reissue", "POST"),
            new AntPathRequestMatcher("/actuator/**")
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 공개 엔드포인트면 바로 패스
        if (skipMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization 헤더 없으면 패스
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request, response,
                    new InsufficientAuthenticationException("EMPTY_ACCESS_TOKEN")
            );
            return;
        }

        if (!header.startsWith("Bearer ")) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request, response,
                    new InsufficientAuthenticationException("BEARER_PREFIX_INVALID")
            );
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtIssuer.parse(token);
            Long userId = claims.get("userId", Long.class);

            // Authentication 객체 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT 인증 성공 - userId: {}", userId);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 Access Token");
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request, response,
                    new InsufficientAuthenticationException("EXPIRED_ACCESS_TOKEN", e)
            );
            return;
        } catch (JwtException e) {
            log.warn("잘못된 Access Token");
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request, response,
                    new BadCredentialsException("INVALID_ACCESS_TOKEN", e)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
