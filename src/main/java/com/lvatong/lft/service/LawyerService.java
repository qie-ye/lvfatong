package com.lvatong.lft.service;

import com.lvatong.lft.common.audit.Auditable;
import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.service.NotificationService;
import com.lvatong.lft.model.dto.*;
import com.lvatong.lft.model.entity.Appointment;
import com.lvatong.lft.model.entity.LawyerProfile;
import com.lvatong.lft.model.entity.User;
import com.lvatong.lft.repository.AppointmentRepository;
import com.lvatong.lft.repository.LawyerProfileRepository;
import com.lvatong.lft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LawyerService {

    private final LawyerProfileRepository lawyerProfileRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 创建/更新律师档案
     */
    @Transactional
    @Auditable(action = "LAWYER_PROFILE_UPDATE", resource = "律师档案")
    @CacheEvict(value = "lawyerListCache", allEntries = true)
    public LawyerProfileResponse createOrUpdateProfile(Long userId, CreateLawyerProfileRequest request) {
        LawyerProfile profile = lawyerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    LawyerProfile lp = new LawyerProfile();
                    lp.setUserId(userId);
                    lp.setRating(0.0);
                    lp.setConsultationCount(0);
                    lp.setVerified(false);
                    lp.setAvailable(true);
                    return lp;
                });

        profile.setRealName(request.getRealName());
        if (request.getLawFirm() != null) profile.setLawFirm(request.getLawFirm());
        if (request.getLicenseNo() != null) profile.setLicenseNo(request.getLicenseNo());
        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getEducation() != null) profile.setEducation(request.getEducation());
        if (request.getSpecialties() != null) profile.setSpecialties(request.getSpecialties());
        if (request.getTags() != null) profile.setTags(request.getTags());
        if (request.getProvince() != null) profile.setProvince(request.getProvince());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getYearsOfExperience() != null) profile.setYearsOfExperience(request.getYearsOfExperience());
        if (request.getConsultationType() != null) {
            profile.setConsultationType(LawyerProfile.ConsultationType.valueOf(request.getConsultationType()));
        }

        profile = lawyerProfileRepository.save(profile);
        log.info("Lawyer profile updated for user {}", userId);
        return LawyerProfileResponse.from(profile);
    }

    /**
     * 获取律师档案
     */
    @Cacheable(value = "lawyerListCache", key = "'profile:' + #lawyerId")
    public LawyerProfileResponse getProfile(Long lawyerId) {
        LawyerProfile profile = lawyerProfileRepository.findById(lawyerId)
                .orElseThrow(() -> new BusinessException("律师档案不存在"));
        return LawyerProfileResponse.from(profile);
    }

    /**
     * 获取当前用户的律师档案
     */
    public LawyerProfileResponse getMyProfile(Long userId) {
        LawyerProfile profile = lawyerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("您尚未创建律师档案"));
        return LawyerProfileResponse.from(profile);
    }

    /**
     * 律师列表（分页，按评分排序）
     */
    @Cacheable(value = "lawyerListCache", key = "'page:' + #page + ':size:' + #size")
    public Page<LawyerProfileResponse> listLawyers(int page, int size) {
        return lawyerProfileRepository.findAllOrderByRating(PageRequest.of(page, size))
                .map(LawyerProfileResponse::from);
    }

    /**
     * 按专业领域搜索律师
     */
    @Cacheable(value = "lawyerListCache", key = "'spec:' + #specialty + ':page:' + #page")
    public Page<LawyerProfileResponse> searchBySpecialty(String specialty, int page, int size) {
        return lawyerProfileRepository.findBySpecialtyOrderByRating(specialty,
                        PageRequest.of(page, size))
                .map(LawyerProfileResponse::from);
    }

    /**
     * 关键词搜索律师
     */
    public Page<LawyerProfileResponse> searchByKeyword(String keyword, int page, int size) {
        return lawyerProfileRepository.searchByKeyword(keyword,
                        PageRequest.of(page, size))
                .map(LawyerProfileResponse::from);
    }

    /**
     * AI推荐律师（基于用户问题标签匹配）
     */
    public List<LawyerProfileResponse> recommendLawyers(String question, int topK) {
        // 简单标签匹配策略：提取问题中的法律领域关键词
        List<String> legalDomains = extractLegalDomains(question);
        if (legalDomains.isEmpty()) {
            return lawyerProfileRepository.findAllOrderByRating(PageRequest.of(0, topK))
                    .map(LawyerProfileResponse::from).getContent();
        }

        // 按匹配度排序
        return lawyerProfileRepository.findAllOrderByRating(PageRequest.of(0, topK * 3))
                .map(LawyerProfileResponse::from)
                .filter(lp -> lp.getSpecialties() != null && lp.getSpecialties().stream()
                        .anyMatch(s -> legalDomains.stream().anyMatch(s::contains)))
                .stream()
                .limit(topK)
                .toList();
    }

    /**
     * 创建预约
     */
    @Transactional
    @Auditable(action = "APPOINTMENT_CREATE", resource = "律师预约")
    public AppointmentResponse createAppointment(Long userId, CreateAppointmentRequest request) {
        LawyerProfile lawyer = lawyerProfileRepository.findById(request.getLawyerId())
                .orElseThrow(() -> new BusinessException("律师不存在"));

        if (!Boolean.TRUE.equals(lawyer.getAvailable())) {
            throw new BusinessException("该律师暂不可预约");
        }

        if (appointmentRepository.existsByLawyerIdAndAppointmentTime(request.getLawyerId(), request.getAppointmentTime())) {
            throw new BusinessException("该时段已被预约");
        }

        Appointment appointment = new Appointment();
        appointment.setUserId(userId);
        appointment.setLawyerId(request.getLawyerId());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setConsultationType(request.getConsultationType());
        appointment.setDescription(request.getDescription());
        appointment.setStatus(Appointment.AppointmentStatus.PENDING);

        appointment = appointmentRepository.save(appointment);
        log.info("Appointment created: user={} lawyer={} time={}", userId, request.getLawyerId(), request.getAppointmentTime());

        return AppointmentResponse.from(appointment, lawyer.getRealName());
    }

    /**
     * 获取用户的预约列表
     */
    public List<AppointmentResponse> getUserAppointments(Long userId) {
        return appointmentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(apt -> {
                    String lawyerName = lawyerProfileRepository.findById(apt.getLawyerId())
                            .map(LawyerProfile::getRealName).orElse("未知律师");
                    return AppointmentResponse.from(apt, lawyerName);
                })
                .toList();
    }

    /**
     * 获取律师的预约列表
     */
    public List<AppointmentResponse> getLawyerAppointments(Long lawyerId) {
        return appointmentRepository.findByLawyerIdOrderByCreatedAtDesc(lawyerId)
                .stream()
                .map(apt -> AppointmentResponse.from(apt, null))
                .toList();
    }

    /**
     * 确认预约
     */
    @Transactional
    @Auditable(action = "APPOINTMENT_CONFIRM", resource = "律师预约")
    public AppointmentResponse confirmAppointment(Long lawyerId, Long appointmentId) {
        Appointment apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("预约不存在"));
        if (!apt.getLawyerId().equals(lawyerId)) {
            throw new BusinessException("无权操作此预约");
        }
        apt.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        apt = appointmentRepository.save(apt);
        String lawyerName = lawyerProfileRepository.findById(lawyerId)
                .map(LawyerProfile::getRealName).orElse("律师");
        notificationService.send(apt.getUserId(), "APPOINTMENT",
                "预约已确认",
                "您与律师" + lawyerName + "的预约已确认，预约时间：" + apt.getAppointmentTime() + "。");
        return AppointmentResponse.from(apt, lawyerName);
    }

    /**
     * 取消预约
     */
    @Transactional
    @Auditable(action = "APPOINTMENT_CANCEL", resource = "律师预约")
    public AppointmentResponse cancelAppointment(Long userId, Long appointmentId, String reason) {
        Appointment apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("预约不存在"));
        if (!apt.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此预约");
        }
        if (apt.getStatus() == Appointment.AppointmentStatus.COMPLETED) {
            throw new BusinessException("已完成的预约无法取消");
        }
        apt.setStatus(Appointment.AppointmentStatus.CANCELLED);
        apt.setCancelReason(reason);
        apt = appointmentRepository.save(apt);
        String lawyerName = lawyerProfileRepository.findById(apt.getLawyerId())
                .map(LawyerProfile::getRealName).orElse("");
        return AppointmentResponse.from(apt, lawyerName);
    }

    private List<String> extractLegalDomains(String question) {
        java.util.Map<String, List<String>> synonymMap = java.util.Map.ofEntries(
                java.util.Map.entry("劳动", List.of("劳动", "工资", "工伤", "裁员", "解雇", "辞退", "劳动合同", "仲裁", "社保", "加班", "五险一金")),
                java.util.Map.entry("合同", List.of("合同", "违约", "合约", "协议", "定金", "订金", "欺诈")),
                java.util.Map.entry("婚姻", List.of("婚姻", "离婚", "抚养", "财产分割", "家庭", "家暴", "出轨", "婚前")),
                java.util.Map.entry("房产", List.of("房产", "房屋", "楼盘", "购房", "租房", "物业", "土地", "拆迁", "二手房")),
                java.util.Map.entry("刑事", List.of("刑事", "犯罪", "诈骗", "盗窃", "伤害", "逮捕", "拘留", "自首", "故意")),
                java.util.Map.entry("知识产权", List.of("知识产权", "专利", "版权", "著作权", "商标", "侵权", "抄袭")),
                java.util.Map.entry("公司", List.of("公司", "股东", "股权", "注册", "清算", "并购", "法人", "营业执照")),
                java.util.Map.entry("交通事故", List.of("交通事故", "车祸", "肇事", "赔偿", "保险理赔", "驾驶")),
                java.util.Map.entry("医疗", List.of("医疗", "医院", "手术", "误诊", "医疗事故", "医生"))
        );
        java.util.Set<String> matched = new java.util.LinkedHashSet<>();
        String q = question;
        for (java.util.Map.Entry<String, List<String>> entry : synonymMap.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (q.contains(keyword)) {
                    matched.add(entry.getKey());
                    break;
                }
            }
        }
        return new java.util.ArrayList<>(matched);
    }
}
