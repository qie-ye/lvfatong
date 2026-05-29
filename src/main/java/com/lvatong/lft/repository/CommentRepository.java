package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTargetTypeAndTargetIdAndParentIdIsNullOrderByCreatedAtDesc(
            Comment.TargetType targetType, Long targetId);

    List<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            Comment.TargetType targetType, Long targetId);

    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    long countByTargetTypeAndTargetId(Comment.TargetType targetType, Long targetId);
}