package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByTeamIdAndChannelTypeOrderByCreatedAtDesc(Long teamId, Message.ChannelType channelType, Pageable pageable);

    Page<Message> findByChannelTypeAndChannelIdOrderByCreatedAtDesc(Message.ChannelType channelType, Long channelId, Pageable pageable);

    List<Message> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    @Query("SELECT m FROM Message m WHERE m.teamId = :teamId AND m.channelType = 'TEAM' ORDER BY m.createdAt DESC")
    Page<Message> findTeamMessages(@Param("teamId") Long teamId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE (m.senderId = :userId OR m.receiverId = :userId) AND m.channelType = 'PRIVATE' ORDER BY m.createdAt DESC")
    Page<Message> findPrivateMessages(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.teamId = :teamId AND m.channelType = 'TEAM' AND m.id > :lastReadMessageId")
    long countUnreadTeamMessages(@Param("teamId") Long teamId, @Param("lastReadMessageId") Long lastReadMessageId);
}