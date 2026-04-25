package com.lvatong.lft.config;

import com.lvatong.lft.model.entity.LawyerProfile;
import com.lvatong.lft.model.entity.User;
import com.lvatong.lft.repository.LawyerProfileRepository;
import com.lvatong.lft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        initAdmin();
        initLawyers();
    }

    private void initAdmin() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("管理员");
            admin.setRole(User.UserRole.ADMIN);
            userRepository.save(admin);
            log.info("默认管理员账户已创建: admin / admin123");
        } else {
            log.debug("管理员账户已存在，跳过初始化");
        }
    }

    private void initLawyers() {
        long verifiedCount = lawyerProfileRepository.findByVerifiedTrueAndAvailableTrue(
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        if (verifiedCount > 0) {
            log.debug("律师档案已存在（{}位可用律师），跳过初始化", verifiedCount);
            return;
        }
        // 清除可能存在的不完整数据
        if (lawyerProfileRepository.count() > 0) {
            log.info("发现不完整的律师档案数据，清除后重新初始化");
            lawyerProfileRepository.deleteAll();
        }

        String defaultPwd = passwordEncoder.encode("lawyer123");
        List<LawyerSeed> seeds = List.of(
            new LawyerSeed("lawyer_wang",  "王建国", "京衡律师事务所", "11101201510012345",
                "资深劳动法律师，擅长处理劳动合同纠纷、工伤赔偿、经济补偿金等劳动争议案件。曾为数百名劳动者维权成功，累计挽回经济损失超过500万元。",
                "中国政法大学 法学硕士", "劳动法,工伤赔偿,经济补偿金,劳动合同,社保纠纷",
                "劳动仲裁,维权专家,免费咨询", "北京", "北京", 15, 9.2, 186, LawyerProfile.ConsultationType.BOTH),
            new LawyerSeed("lawyer_li",    "李芳",   "锦天城律师事务所", "11101201610023456",
                "专注婚姻家事法律领域十余年，擅长离婚诉讼、财产分割、抚养权争议及婚前财产协议拟定。注重调解优先，致力于维护当事人及未成年子女合法权益。",
                "华东政法大学 法学学士", "婚姻法,离婚诉讼,财产分割,抚养权,婚前协议",
                "调解专家,女性权益,家事律师", "上海", "上海", 12, 9.0, 152, LawyerProfile.ConsultationType.ONLINE),
            new LawyerSeed("lawyer_zhang", "张伟",   "大成律师事务所",   "11101201410034567",
                "合同法领域资深律师，处理各类买卖合同、租赁合同、服务合同纠纷。擅长合同审查、风险防范及违约索赔，为企业客户提供常年法律顾问服务。",
                "北京大学 法学博士", "合同法,买卖合同,违约索赔,合同审查,法律顾问",
                "企业法务,合同专家,风险防范", "北京", "北京", 18, 9.4, 230, LawyerProfile.ConsultationType.BOTH),
            new LawyerSeed("lawyer_chen",  "陈晓红", "国浩律师事务所",   "11101201710045678",
                "知识产权专业律师，擅长专利侵权诉讼、商标注册与保护、著作权纠纷及商业秘密保护。曾代理多起知名知识产权案件，具有丰富的诉讼实战经验。",
                "清华大学 法学硕士", "知识产权,专利侵权,商标保护,著作权,商业秘密",
                "知产专家,诉讼经验丰富", "广东", "深圳", 10, 8.8, 98, LawyerProfile.ConsultationType.ONLINE),
            new LawyerSeed("lawyer_liu",   "刘明",   "盈科律师事务所",   "11101201310056789",
                "刑事辩护资深律师，办理各类刑事案件三百余件。擅长经济犯罪辩护、职务犯罪辩护、刑事合规审查，多起案件取得无罪或缓刑结果。",
                "中国政法大学 法学博士", "刑事辩护,经济犯罪,职务犯罪,刑事合规,取保候审",
                "无罪辩护,缓刑专家,刑事合规", "四川", "成都", 20, 9.6, 276, LawyerProfile.ConsultationType.BOTH),
            new LawyerSeed("lawyer_zhao",  "赵雪",   "德恒律师事务所",   "11101201810067890",
                "房产法律专业律师，擅长商品房买卖纠纷、二手房交易风险防范、物业纠纷及拆迁补偿。为购房者提供全流程法律保障服务。",
                "武汉大学 法学硕士", "房产法,商品房纠纷,二手房交易,物业纠纷,拆迁补偿",
                "购房维权,房产专家", "湖北", "武汉", 8, 8.6, 76, LawyerProfile.ConsultationType.ONLINE),
            new LawyerSeed("lawyer_sun",   "孙磊",   "中伦律师事务所",   "11101201610078901",
                "公司法领域资深律师，擅长公司治理、股权纠纷、并购重组及企业合规。为多家上市公司及创业公司提供常年法律顾问服务。",
                "中国人民大学 法学硕士", "公司法,股权纠纷,并购重组,公司治理,企业合规",
                "上市顾问,股权专家", "浙江", "杭州", 14, 9.2, 164, LawyerProfile.ConsultationType.BOTH),
            new LawyerSeed("lawyer_huang", "黄丽",   "君合律师事务所",   "11101201910089012",
                "交通事故与保险理赔专业律师，擅长交通事故责任认定、人身损害赔偿、保险合同纠纷。为事故受害者争取最大赔偿，累计获赔金额超过2000万元。",
                "西南政法大学 法学学士", "交通事故,人身损害,保险理赔,责任认定,赔偿计算",
                "理赔专家,损害赔偿", "重庆", "重庆", 6, 8.4, 54, LawyerProfile.ConsultationType.ONLINE)
        );

        for (LawyerSeed seed : seeds) {
            User user = userRepository.findByUsername(seed.username).orElseGet(() -> {
                User u = new User();
                u.setUsername(seed.username);
                u.setPassword(defaultPwd);
                u.setNickname(seed.realName + "律师");
                u.setRole(User.UserRole.LAWYER);
                return userRepository.save(u);
            });

            LawyerProfile profile = new LawyerProfile();
            profile.setUserId(user.getId());
            profile.setRealName(seed.realName);
            profile.setLawFirm(seed.lawFirm);
            profile.setLicenseNo(seed.licenseNo);
            profile.setBio(seed.bio);
            profile.setEducation(seed.education);
            profile.setSpecialties(seed.specialties);
            profile.setTags(seed.tags);
            profile.setProvince(seed.province);
            profile.setCity(seed.city);
            profile.setYearsOfExperience(seed.years);
            profile.setRating(seed.rating);
            profile.setConsultationCount(seed.consultCount);
            profile.setVerified(true);
            profile.setAvailable(true);
            profile.setConsultationType(seed.consultType);
            lawyerProfileRepository.save(profile);
        }
        log.info("律师种子数据已创建：{} 位律师，默认密码 lawyer123", seeds.size());
    }

    private record LawyerSeed(String username, String realName, String lawFirm, String licenseNo,
                              String bio, String education, String specialties, String tags,
                              String province, String city, int years, double rating, int consultCount,
                              LawyerProfile.ConsultationType consultType) {}
}
