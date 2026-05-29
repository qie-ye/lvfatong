package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_collaborations", indexes = {
        @Index(name = "idx_case_collaborations_case_id", columnList = "caseId"),
        @Index(name = "idx_case_collaborations_team_id", columnList = "teamId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_case_team", columnNames = {"case_id", "team_id"})
})
public class CaseCollaboration extends BaseEntity {

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "shared_by", nullable = false)
    private Long sharedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Permission permission = Permission.VIEW;

    public enum Permission {
        VIEW,   // 查看
        EDIT,   // 编辑
        ADMIN   // 管理
    }
}