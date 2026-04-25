package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "lawyer_reviews", indexes = {
        @Index(name = "idx_lawyer_id", columnList = "lawyerId"),
        @Index(name = "idx_user_id", columnList = "userId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_lawyer", columnNames = {"userId", "lawyerId"})
})
public class LawyerReview extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long lawyerId;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(length = 1000)
    private String comment;

    @Column(length = 50)
    private String serviceType; // ONLINE, OFFLINE, PHONE
}
