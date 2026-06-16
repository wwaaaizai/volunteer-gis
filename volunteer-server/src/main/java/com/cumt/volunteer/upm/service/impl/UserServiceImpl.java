package com.cumt.volunteer.upm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cumt.volunteer.common.JwtUtils;
import com.cumt.volunteer.entity.User;
import com.cumt.volunteer.upm.mapper.UserMapper;
import com.cumt.volunteer.upm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public void register(String studentId, String password, String name, String phone) {
        // 检查学号唯一性
        User exist = findByStudentId(studentId);
        if (exist != null) {
            throw new RuntimeException("该学号已注册");
        }
        User user = new User();
        user.setStudentId(studentId);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);
        user.setPhone(phone);
        user.setRole("student");
        user.setTotalHours(java.math.BigDecimal.ZERO);
        save(user);
    }

    @Override
    public String login(String studentId, String password) {
        User user = findByStudentId(studentId);
        if (user == null) {
            throw new RuntimeException("学号或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("学号或密码错误");
        }
        return jwtUtils.generateToken(user.getId(), user.getStudentId(), user.getRole());
    }

    @Override
    public User findByStudentId(String studentId) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getStudentId, studentId));
    }
}
