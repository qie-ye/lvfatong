package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "team_invitations", indexes = {
        @Index(name = "idx_team_invitations_team_id", columnList = "teamId"),
        @Index(name = "idx_team_invitations_invitee_id", columnList = "inviteeId"),
        @Index(name = "idx_team_invitations_status", columnList = "status")
})
public class TeamInvitation extends BaseEntity {

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "inviter_id", nullable = false)
    private Long inviterId;

    @Column(name = "invitee_id")
    private Long inviteeId;

    @Column(name = "invitee_phone", length = 20)
    private String inviteePhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public enum InvitationStatus {
        PENDING,    // 待处理
        ACCEPTED,   // 已接受
        REJECTED,   // 已拒绝
        EXPIRED     // 已过期
    }
}