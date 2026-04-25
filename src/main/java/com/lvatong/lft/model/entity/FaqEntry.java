package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "faq_entries")
public class FaqEntry extends BaseEntity {

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "JSON")
    private String tags;

    @Column(nullable = false)
    private Boolean enabled = true;
}
