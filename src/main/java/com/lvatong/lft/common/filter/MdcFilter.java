package com.lvatong.lft.common.filter;

import com.lvatong.lft.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MdcFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            MDC.put("requestId", requestId);
            MDC.put("traceId", requestId);

            String token = extractToken(request);
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                try {
                    Long userId = jwtTokenProvider.getUserIdFromToken(token);
                    MDC.put("userId", userId.toString());
                } catch (Exception e) {
                    log.debug("MdcFilter: could not extract userId from token: {}", e.getMessage());
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
