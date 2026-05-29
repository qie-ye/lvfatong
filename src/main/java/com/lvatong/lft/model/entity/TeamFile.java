package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "team_files", indexes = {
        @Index(name = "idx_team_files_team_id", columnList = "teamId"),
        @Index(name = "idx_team_files_case_id", columnList = "caseId"),
        @Index(name = "idx_team_files_uploader_id", columnList = "uploaderId")
})
public class TeamFile extends BaseEntity {

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "case_id")
    private Long caseId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;
}