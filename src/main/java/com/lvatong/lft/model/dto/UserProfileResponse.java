package com.lvatong.lft.model.dto;

import com.lvatong.lft.model.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String role;
    private LocalDateTime createdAt;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
