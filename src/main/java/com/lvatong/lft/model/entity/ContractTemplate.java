package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "contract_templates")
public class ContractTemplate extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 50)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 50)
    private String applicableLaw;

    @Column
    private Boolean enabled;
}
