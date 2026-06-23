package com.cumt.volunteer.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cumt.volunteer.aca.mapper.ActivityMapper;
import com.cumt.volunteer.entity.Activity;
import com.cumt.volunteer.entity.User;
import com.cumt.volunteer.upm.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据初始化：确保管理员账号存在，并创建种子活动
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
        // 1. 创建管理员账号
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

        // 2. 若活动表为空，插入种子活动（中国矿业大学南湖校区）
        Long count = activityMapper.selectCount(null);
        if (count == 0) {
            List<Activity> seeds = List.of(
                    buildActivity("图书馆整理", "协助整理图书馆一楼藏书，分类上架",
                            "图书馆一楼", 117.2036, 34.2173, 50),
                    buildActivity("博学楼导览", "为新生提供博学楼A区教室指引服务",
                            "博学楼A区", 117.2050, 34.2185, 30),
                    buildActivity("体育场布置", "协助南湖体育场活动场地布置与器材搬运",
                            "南湖体育场", 117.2080, 34.2150, 100),
                    buildActivity("计算机实验室维护", "协助计算机学院实验楼机房设备清洁与维护",
                            "计算机学院实验楼", 117.2065, 34.2160, 15)
            );
            seeds.forEach(activityMapper::insert);
            log.info("种子活动已创建: {} 条", seeds.size());
        }
    }

    private static Activity buildActivity(String title, String desc, String location,
                                          double lng, double lat, int max) {
        Activity a = new Activity();
        a.setTitle(title);
        a.setDescription(desc);
        a.setLocationName(location);
        a.setLongitude(BigDecimal.valueOf(lng));
        a.setLatitude(BigDecimal.valueOf(lat));
        a.setMaxParticipants(max);
        a.setSignedCount(0);
        a.setStatus("published");
        a.setStartTime(LocalDateTime.now().plusDays(7));
        a.setEndTime(LocalDateTime.now().plusDays(7).plusHours(3));
        return a;
    }
}
