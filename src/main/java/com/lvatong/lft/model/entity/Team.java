package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "teams", indexes = {
        @Index(name = "idx_teams_owner_id", columnList = "ownerId"),
        @Index(name = "idx_teams_invite_code", columnList = "inviteCode"),
        @Index(name = "idx_teams_status", columnList = "status")
})
public class Team extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "invite_code", unique = true, length = 20)
    private String inviteCode;

    @Column(name = "max_members")
    private Integer maxMembers = 50;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamStatus status = TeamStatus.ACTIVE;

    public enum TeamStatus {
        ACTIVE,     // 活跃
        DISBANDED   // 已解散
    }
}