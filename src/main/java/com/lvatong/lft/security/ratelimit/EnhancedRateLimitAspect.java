package com.lvatong.lft.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class EnhancedRateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    @Value("${lvatong.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${lvatong.security.rate-limit.default-limit:100}")
    private int defaultLimit;

    @Value("${lvatong.security.rate-limit.default-window:60}")
    private int defaultWindow;

    /**
     * 增强限流切面
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (!rateLimitEnabled) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = getCurrentRequest();
        String key = buildRateLimitKey(request, rateLimit);
        int limit = rateLimit.limit() > 0 ? rateLimit.limit() : defaultLimit;
        int window = rateLimit.window() > 0 ? rateLimit.window() : defaultWindow;

        try {
            // 使用Redis令牌桶算法
            String redisKey = "rate_limit:" + key;
            Long current = redisTemplate.opsForValue().increment(redisKey);
            
            if (current == null || current == 1) {
                // 第一次访问，设置过期时间
                redisTemplate.expire(redisKey, window, TimeUnit.SECONDS);
            }

            if (current != null && current > limit) {
                log.warn("API限流触发: key={}, limit={}, current={}", key, limit, current);
                throw new RateLimitException("请求过于频繁，请稍后再试");
            }

            return joinPoint.proceed();
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("限流检查异常", e);
            // 降级处理，允许请求通过
            return joinPoint.proceed();
        }
    }

    /**
     * 构建限流Key
     */
    private String buildRateLimitKey(HttpServletRequest request, RateLimit rateLimit) {
        String ip = getClientIp(request);
        String path = request.getRequestURI();
        
        if (rateLimit.keyType() == RateLimit.KeyType.IP) {
            return ip + ":" + path;
        } else if (rateLimit.keyType() == RateLimit.KeyType.USER) {
            String userId = request.getHeader("X-User-Id");
            return (userId != null ? userId : ip) + ":" + path;
        } else {
            return path;
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new RuntimeException("无法获取当前请求");
        }
        return attributes.getRequest();
    }

    /**
     * 限流异常
     */
    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }
}