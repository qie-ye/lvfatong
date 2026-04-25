package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.dto.AdminOverviewResponse;
import com.lvatong.lft.model.dto.DailyStatDto;
import com.lvatong.lft.model.dto.FeedbackStatsResponse;
import com.lvatong.lft.model.dto.NameValueDto;
import com.lvatong.lft.model.entity.AnswerFeedback;
import com.lvatong.lft.repository.AnswerFeedbackRepository;
import com.lvatong.lft.repository.ChatMessageRepository;
import com.lvatong.lft.repository.ChatSessionRepository;
import com.lvatong.lft.repository.LawyerProfileRepository;
import com.lvatong.lft.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@Tag(name = "管理看板", description = "ADMIN专属数据统计（路由层已鉴权）")
public class AdminController {

    private final UserRepository userRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final AnswerFeedbackRepository feedbackRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @GetMapping("/overview")
    @Operation(summary = "KPI概览：总用户/总对话/今日活跃/满意率")
    public ApiResult<AdminOverviewResponse> overview() {
        long totalUsers    = userRepository.count();
        long totalSessions = chatSessionRepository.count();
        long todayActive   = chatSessionRepository.countActiveUsersSince(
                LocalDate.now().atStartOfDay());

        long good = feedbackRepository.countByRating(AnswerFeedback.Rating.GOOD);
        long bad  = feedbackRepository.countByRating(AnswerFeedback.Rating.BAD);
        double satisfactionRate = (good + bad) == 0 ? 0.0
                : Math.round((double) good / (good + bad) * 1000) / 10.0;

        return ApiResult.success(new AdminOverviewResponse(totalUsers, totalSessions, todayActive, satisfactionRate));
    }

    @GetMapping("/users")
    @Operation(summary = "近30天注册趋势")
    public ApiResult<List<DailyStatDto>> userTrend() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Object[]> rows = userRepository.countRegistrationByDay(since);
        List<DailyStatDto> result = rows.stream()
                .map(r -> new DailyStatDto(r[0].toString(), ((Number) r[1]).longValue()))
                .collect(Collectors.toList());
        return ApiResult.success(result);
    }

    @GetMapping("/intents")
    @Operation(summary = "意图类型分布（关键词分类）")
    public ApiResult<List<NameValueDto>> intentDistribution() {
        List<Object[]> rows = chatMessageRepository.countByIntentKeyword();
        List<NameValueDto> result = rows.stream()
                .map(r -> new NameValueDto(r[0].toString(), ((Number) r[1]).longValue()))
                .collect(Collectors.toList());
        return ApiResult.success(result);
    }

    @GetMapping("/feedback")
    @Operation(summary = "近7天每日好评率")
    public ApiResult<List<DailyStatDto>> feedbackTrend() {
        List<Object[]> rows = feedbackRepository.findDailyStats();
        List<DailyStatDto> result = rows.stream()
                .limit(7)
                .map(r -> {
                    String day    = r[0].toString();
                    long goodCnt  = ((Number) r[1]).longValue();
                    long total    = ((Number) r[2]).longValue();
                    long rate     = total == 0 ? 0 : Math.round((double) goodCnt / total * 100);
                    return new DailyStatDto(day, rate);
                })
                .collect(Collectors.toList());
        return ApiResult.success(result);
    }

    @GetMapping("/lawyers")
    @Operation(summary = "律师咨询量TOP10")
    public ApiResult<List<NameValueDto>> lawyerTop10() {
        List<Object[]> rows = lawyerProfileRepository
                .findTop10ByConsultationCount(PageRequest.of(0, 10));
        List<NameValueDto> result = rows.stream()
                .map(r -> {
                    String name  = r[1] != null ? r[1].toString() : "未知律师";
                    long count   = ((Number) r[2]).longValue();
                    return new NameValueDto(name, count);
                })
                .collect(Collectors.toList());
        return ApiResult.success(result);
    }
}
