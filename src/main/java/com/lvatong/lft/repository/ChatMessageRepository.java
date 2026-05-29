package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    void deleteBySessionId(Long sessionId);
    long countBySessionId(Long sessionId);

    @Query(value =
        "SELECT intent_name, COUNT(*) AS cnt FROM (" +
        "  SELECT CASE " +
        "    WHEN content LIKE '%合同%' THEN '合同问题' " +
        "    WHEN content LIKE '%起诉%' OR content LIKE '%打官司%' THEN '诉讼咨询' " +
        "    WHEN content LIKE '%法律%' OR content LIKE '%法规%' THEN '法律咨询' " +
        "    ELSE '其他咨询' END AS intent_name " +
        "  FROM chat_messages WHERE role = 'USER'" +
        ") t GROUP BY intent_name",
        nativeQuery = true)
    List<Object[]> countByIntentKeyword();

    @Query(value = "SELECT DATE(created_at) as day, COUNT(*) as cnt FROM chat_messages WHERE created_at >= :since GROUP BY DATE(created_at) ORDER BY day", nativeQuery = true)
    List<Object[]> countByDaySince(@Param("since") LocalDateTime since);

    @Query(value = "SELECT HOUR(created_at) as hour, COUNT(*) as cnt FROM chat_messages WHERE created_at >= :since GROUP BY HOUR(created_at) ORDER BY hour", nativeQuery = true)
    List<Object[]> countByHourSince(@Param("since") LocalDateTime since);

    long countByCreatedAtAfter(LocalDateTime since);
}
