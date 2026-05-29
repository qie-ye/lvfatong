package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_activity_logs", indexes = {
        @Index(name = "idx_case_activity_logs_case_id", columnList = "caseId"),
        @Index(name = "idx_case_activity_logs_user_id", columnList = "userId"),
        @Index(name = "idx_case_activity_logs_created_at", columnList = "createdAt")
})
public class CaseActivityLog extends BaseEntity {

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(columnDefinition = "JSON")
    private String details;
}