package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "team_members", indexes = {
        @Index(name = "idx_team_members_team_id", columnList = "teamId"),
        @Index(name = "idx_team_members_user_id", columnList = "userId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_team_user", columnNames = {"team_id", "user_id"})
})
public class TeamMember extends BaseEntity {

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role = MemberRole.MEMBER;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    public enum MemberRole {
        OWNER,  // 所有者
        ADMIN,  // 管理员
        MEMBER  // 成员
    }
}