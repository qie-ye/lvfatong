package com.lvatong.lft.websocket;

import com.lvatong.lft.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        Long userId = jwtTokenProvider.getUserIdFromToken(token);
                        accessor.setUser(() -> userId.toString());
                        log.debug("[WS] STOMP CONNECT authenticated userId={}", userId);
                    }
                } catch (Exception e) {
                    log.warn("[WS] STOMP CONNECT JWT validation failed: {}", e.getMessage());
                }
            }
        }
        return message;
    }
}
