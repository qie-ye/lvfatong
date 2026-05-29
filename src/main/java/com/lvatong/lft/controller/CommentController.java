package com.lvatong.lft.controller;

import com.lvatong.lft.collaboration.CommentService;
import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.entity.Comment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "评论管理", description = "案件/任务评论、回复功能")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "添加评论")
    public ApiResult<Comment> addComment(@RequestBody AddCommentRequest request) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(commentService.addComment(userId, request.getTargetType(),
                request.getTargetId(), request.getContent(), request.getParentId()));
    }

    @GetMapping
    @Operation(summary = "获取评论列表")
    public ApiResult<List<Comment>> getComments(
            @RequestParam Comment.TargetType targetType,
            @RequestParam Long targetId,
            @RequestParam(defaultValue = "false") boolean all) {
        if (all) {
            return ApiResult.success(commentService.getAllComments(targetType, targetId));
        }
        return ApiResult.success(commentService.getComments(targetType, targetId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取评论详情")
    public ApiResult<Comment> getComment(@PathVariable Long id) {
        return ApiResult.success(commentService.getComment(id));
    }

    @GetMapping("/{id}/replies")
    @Operation(summary = "获取评论回复")
    public ApiResult<List<Comment>> getReplies(@PathVariable Long id) {
        return ApiResult.success(commentService.getReplies(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑评论")
    public ApiResult<Comment> updateComment(@PathVariable Long id, @RequestBody UpdateCommentRequest request) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(commentService.updateComment(id, userId, request.getContent()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论")
    public ApiResult<Void> deleteComment(@PathVariable Long id) {
        Long userId = 1L; // 临时硬编码
        commentService.deleteComment(id, userId);
        return ApiResult.success(null);
    }

    @GetMapping("/count")
    @Operation(summary = "获取评论数量")
    public ApiResult<Long> getCommentCount(
            @RequestParam Comment.TargetType targetType,
            @RequestParam Long targetId) {
        return ApiResult.success(commentService.getCommentCount(targetType, targetId));
    }

    @Data
    public static class AddCommentRequest {
        private Comment.TargetType targetType;
        private Long targetId;
        private String content;
        private Long parentId;
    }

    @Data
    public static class UpdateCommentRequest {
        private String content;
    }
}