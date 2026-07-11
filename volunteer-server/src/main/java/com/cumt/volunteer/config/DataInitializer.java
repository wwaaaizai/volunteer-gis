package com.cumt.volunteer.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cumt.volunteer.entity.Activity;
import com.cumt.volunteer.entity.User;
import com.cumt.volunteer.aca.mapper.ActivityMapper;
import com.cumt.volunteer.upm.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 数据初始化：确保管理员、学生测试账号及演示活动存在
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdmin();
        initStudent();
        initOrganizer();
        initDemoActivities();
    }

    private void initAdmin() {
        User exist = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "admin"));
        if (exist == null) {
            User admin = new User();
            admin.setStudentId("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("系统管理员");
            admin.setRole("admin");
            admin.setTotalHours(BigDecimal.ZERO);
            userMapper.insert(admin);
            log.info("管理员账号已创建: admin / admin123");
        }
    }

    private void initStudent() {
        User exist = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "student"));
        if (exist == null) {
            User student = new User();
            student.setStudentId("student");
            student.setPassword(passwordEncoder.encode("123456"));
            student.setName("张同学");
            student.setRole("student");
            student.setTotalHours(BigDecimal.ZERO);
            userMapper.insert(student);
            log.info("学生测试账号已创建: student / 123456");
        }
    }

    private void initOrganizer() {
        User exist = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "organizer"));
        if (exist == null) {
            User organizer = new User();
            organizer.setStudentId("organizer");
            organizer.setPassword(passwordEncoder.encode("organizer123"));
            organizer.setName("李组织者");
            organizer.setRole("organizer");
            organizer.setOrganization("校团委");
            organizer.setTotalHours(BigDecimal.ZERO);
            userMapper.insert(organizer);
            log.info("组织者测试账号已创建: organizer / organizer123");
        }
    }

    private void initDemoActivities() {
        long count = activityMapper.selectCount(new LambdaQueryWrapper<>());
        if (count > 0) {
            log.info("数据库中已有 {} 个活动，跳过演示数据初始化", count);
            return;
        }

        // 创建4个演示活动（WGS-84坐标，MapService中会转为GCJ-02）
        Activity a1 = buildActivity("图书馆整理", "整理图书馆一楼藏书，分类归架，协助馆员维护阅览环境。",
                "图书馆一楼", 117.2036, 34.2173, 50, "published", "campus", "室内,整理,图书馆");
        Activity a2 = buildActivity("博学楼导览", "为新生提供博学楼教室导览服务，引导参观并讲解楼层分布。",
                "博学楼A区", 117.2050, 34.2185, 30, "published", "campus", "导览,新生,教学楼");
        Activity a3 = buildActivity("体育场布置", "协助校运会场地布置，搬运器材、设置赛道标志、维护现场秩序。",
                "南湖体育场", 117.2080, 34.2150, 100, "published", "campus", "运动会,户外,体力");
        Activity a4 = buildActivity("计算机实验室维护", "协助清理计算机实验室，检查设备运行状态，整理线缆与外设。",
                "计算机学院实验楼", 117.2065, 34.2160, 15, "published", "campus", "计算机,维护,室内");

        activityMapper.insert(a1);
        activityMapper.insert(a2);
        activityMapper.insert(a3);
        activityMapper.insert(a4);
        log.info("已创建 4 个演示活动（published 状态，可在地图查看和报名）");
    }

    private Activity buildActivity(String title, String desc, String location,
                                    double lng, double lat, int max, String status,
                                    String category, String tags) {
        Activity a = new Activity();
        a.setTitle(title);
        a.setDescription(desc);
        a.setLocationName(location);
        a.setLongitude(BigDecimal.valueOf(lng));
        a.setLatitude(BigDecimal.valueOf(lat));
        a.setMaxParticipants(max);
        a.setSignedCount(0);
        a.setStatus(status);
        a.setCreatorId(1L);
        a.setOrganizerId(1L);
        a.setCategory(category);
        a.setTags(tags);
        return a;
    }
}
