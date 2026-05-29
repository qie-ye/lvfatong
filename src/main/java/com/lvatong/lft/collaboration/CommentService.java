package com.lvatong.lft.collaboration;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.entity.Comment;
import com.lvatong.lft.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    /**
     * 添加评论
     */
    @Transactional
    public Comment addComment(Long userId, Comment.TargetType targetType, Long targetId, 
                              String content, Long parentId) {
        // 如果是回复，检查父评论是否存在
        if (parentId != null) {
            commentRepository.findById(parentId)
                    .orElseThrow(() -> new BusinessException("父评论不存在"));
        }

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setTargetType(targetType);
        comment.setTargetId(targetId);
        comment.setContent(content);
        comment.setParentId(parentId);

        comment = commentRepository.save(comment);
        log.info("评论添加成功: targetType={}, targetId={}, userId={}", targetType, targetId, userId);
        return comment;
    }

    /**
     * 获取评论列表（只返回顶级评论）
     */
    public List<Comment> getComments(Comment.TargetType targetType, Long targetId) {
        return commentRepository.findByTargetTypeAndTargetIdAndParentIdIsNullOrderByCreatedAtDesc(
                targetType, targetId);
    }

    /**
     * 获取所有评论（包括回复）
     */
    public List<Comment> getAllComments(Comment.TargetType targetType, Long targetId) {
        return commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                targetType, targetId);
    }

    /**
     * 获取评论回复
     */
    public List<Comment> getReplies(Long parentId) {
        return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    /**
     * 获取评论详情
     */
    public Comment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("评论不存在"));
    }

    /**
     * 编辑评论
     */
    @Transactional
    public Comment updateComment(Long commentId, Long userId, String content) {
        Comment comment = getComment(commentId);

        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("只能编辑自己的评论");
        }

        comment.setContent(content);
        comment = commentRepository.save(comment);
        log.info("评论更新成功: commentId={}", commentId);
        return comment;
    }

    /**
     * 删除评论
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = getComment(commentId);

        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的评论");
        }

        // 删除子评论
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
        commentRepository.deleteAll(replies);

        commentRepository.delete(comment);
        log.info("评论删除成功: commentId={}", commentId);
    }

    /**
     * 获取评论数量
     */
    public long getCommentCount(Comment.TargetType targetType, Long targetId) {
        return commentRepository.countByTargetTypeAndTargetId(targetType, targetId);
    }
}