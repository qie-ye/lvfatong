package com.lvatong.lft.collaboration;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.entity.TeamFile;
import com.lvatong.lft.model.entity.TeamMember;
import com.lvatong.lft.repository.TeamFileRepository;
import com.lvatong.lft.team.TeamMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamFileService {

    private final TeamFileRepository teamFileRepository;
    private final TeamMemberService teamMemberService;

    @Value("${team.file.upload-dir:./uploads/team-files}")
    private String uploadDir;

    @Value("${team.file.max-size:10485760}") // 10MB
    private long maxSize;

    /**
     * 上传文件
     */
    @Transactional
    public TeamFile uploadFile(Long teamId, Long userId, MultipartFile file, Long caseId) {
        // 检查是否是团队成员
        if (!teamMemberService.isTeamMember(teamId, userId)) {
            throw new BusinessException("您不是该团队成员");
        }

        // 检查文件大小
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小超过限制");
        }

        try {
            // 创建上传目录
            Path uploadPath = Paths.get(uploadDir, String.valueOf(teamId));
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            // 保存文件信息
            TeamFile teamFile = new TeamFile();
            teamFile.setTeamId(teamId);
            teamFile.setCaseId(caseId);
            teamFile.setName(originalFilename);
            teamFile.setFileUrl(filePath.toString());
            teamFile.setFileSize(file.getSize());
            teamFile.setFileType(file.getContentType());
            teamFile.setUploaderId(userId);

            teamFile = teamFileRepository.save(teamFile);
            log.info("文件上传成功: teamId={}, filename={}", teamId, originalFilename);
            return teamFile;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取团队文件列表
     */
    public List<TeamFile> getTeamFiles(Long teamId, Long caseId) {
        if (caseId != null) {
            return teamFileRepository.findByTeamIdAndCaseIdOrderByCreatedAtDesc(teamId, caseId);
        }
        return teamFileRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
    }

    /**
     * 获取文件详情
     */
    public TeamFile getFile(Long fileId) {
        return teamFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
    }

    /**
     * 删除文件
     */
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        TeamFile teamFile = getFile(fileId);

        // 检查权限（上传者或团队管理员可以删除）
        if (!teamFile.getUploaderId().equals(userId)) {
            TeamMember.MemberRole role = teamMemberService.getMemberRole(teamFile.getTeamId(), userId);
            if (role == null || role == TeamMember.MemberRole.MEMBER) {
                throw new BusinessException("只有上传者或管理员可以删除文件");
            }
        }

        // 删除物理文件
        try {
            Path filePath = Paths.get(teamFile.getFileUrl());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("删除物理文件失败: {}", e.getMessage());
        }

        teamFileRepository.delete(teamFile);
        log.info("文件删除成功: fileId={}", fileId);
    }

    /**
     * 获取团队文件统计
     */
    public FileStats getTeamFileStats(Long teamId) {
        FileStats stats = new FileStats();
        stats.setFileCount(teamFileRepository.countByTeamId(teamId));
        stats.setTotalSize(teamFileRepository.sumFileSizeByTeamId(teamId));
        return stats;
    }

    /**
     * 文件统计DTO
     */
    @lombok.Data
    public static class FileStats {
        private long fileCount;
        private Long totalSize;
    }
}