package com.cumt.volunteer.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cumt.volunteer.entity.User;
import com.cumt.volunteer.upm.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化：确保管理员账号存在
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User exist = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "admin"));
        if (exist == null) {
            User admin = new User();
            admin.setStudentId("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("系统管理员");
            admin.setRole("admin");
            admin.setTotalHours(java.math.BigDecimal.ZERO);
            userMapper.insert(admin);
            log.info("管理员账号已创建: admin / admin123");
        }
    }
}
