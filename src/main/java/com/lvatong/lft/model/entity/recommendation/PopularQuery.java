package com.lvatong.lft.model.entity.recommendation;

import com.lvatong.lft.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 热门查询实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "popular_queries", indexes = {
    @Index(name = "idx_popular_queries_text", columnList = "queryText", unique = true),
    @Index(name = "idx_popular_queries_domain", columnList = "domain"),
    @Index(name = "idx_popular_queries_count", columnList = "queryCount")
})
public class PopularQuery extends BaseEntity {

    @Column(name = "query_text", nullable = false, length = 500)
    private String queryText;

    @Column(name = "domain", length = 100)
    private String domain;

    @Column(name = "query_count")
    private Integer queryCount = 1;

    @Column(name = "last_queried_at", nullable = false)
    private LocalDateTime lastQueriedAt;
}
