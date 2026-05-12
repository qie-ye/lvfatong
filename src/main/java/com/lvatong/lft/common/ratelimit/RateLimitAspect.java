package com.lvatong.lft.common.ratelimit;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(rateLimit);
        if (key == null) {
            return joinPoint.proceed();
        }

        String redisKey = RATE_LIMIT_PREFIX + key;
        try {
            Long current = redisTemplate.opsForValue().increment(redisKey);
            if (current != null && current == 1) {
                redisTemplate.expire(redisKey, 1, TimeUnit.SECONDS);
            }

            if (current != null && current > rateLimit.permitsPerSecond()) {
                log.warn("Rate limit exceeded: key={} current={}", key, current);
                throw new BusinessException(429, rateLimit.message());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Rate limit check failed, allowing request: {}", e.getMessage());
        }

        return joinPoint.proceed();
    }

    private String buildKey(RateLimit rateLimit) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;

        HttpServletRequest request = attrs.getRequest();
        String dimension = rateLimit.dimension();

        switch (dimension) {
            case "IP" -> {
                return "ip:" + getClientIp(request);
            }
            case "USER" -> {
                String token = extractToken(request);
                if (token != null) {
                    Long userId = jwtTokenProvider.getUserIdFromToken(token);
                    if (userId != null) return "user:" + userId;
                }
                return "ip:" + getClientIp(request);
            }
            case "GLOBAL" -> {
                return "global:" + request.getRequestURI();
            }
            default -> {
                return "ip:" + getClientIp(request);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
