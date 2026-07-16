package com.cumt.volunteer.upm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cumt.volunteer.common.JwtUtils;
import com.cumt.volunteer.entity.OrganizerApply;
import com.cumt.volunteer.entity.User;
import com.cumt.volunteer.upm.mapper.OrganizerApplyMapper;
import com.cumt.volunteer.upm.mapper.UserMapper;
import com.cumt.volunteer.upm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final OrganizerApplyMapper organizerApplyMapper;

    @Override
    @Transactional
    public void register(String studentId, String password, String name, String phone,
                         String grade, String college,
                         boolean applyOrganizer, String organization, String employeeId) {
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
        user.setGrade(grade);
        user.setCollege(college);
        user.setRole("student");
        user.setTotalHours(BigDecimal.ZERO);

        // 组织者申请：暂存机构和工号到用户表，创建审批记录
        if (applyOrganizer) {
            user.setOrganization(organization);
            user.setEmployeeId(employeeId);
            save(user);

            OrganizerApply apply = new OrganizerApply();
            apply.setUserId(user.getId());
            apply.setOrganization(organization);
            apply.setReason("注册时申请成为组织者");
            apply.setStatus("pending");
            organizerApplyMapper.insert(apply);
        } else {
            save(user);
        }
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

    @Override
    public List<OrganizerApply> listPendingApplies() {
        return organizerApplyMapper.selectList(
                new LambdaQueryWrapper<OrganizerApply>()
                        .eq(OrganizerApply::getStatus, "pending")
                        .orderByDesc(OrganizerApply::getCreatedAt)
        );
    }

    @Override
    @Transactional
    public void reviewOrganizerApply(Long applyId, boolean approved, Long reviewerId) {
        OrganizerApply apply = organizerApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new RuntimeException("申请记录不存在");
        }
        if (!"pending".equals(apply.getStatus())) {
            throw new RuntimeException("该申请已处理");
        }

        apply.setStatus(approved ? "approved" : "rejected");
        apply.setReviewedBy(reviewerId);
        organizerApplyMapper.updateById(apply);

        // 审批通过 → 更新用户角色为 organizer
        if (approved) {
            User user = getById(apply.getUserId());
            if (user != null) {
                user.setRole("organizer");
                updateById(user);
            }
        }
    }

    @Override
    public void updateProfile(Long userId, String name, String phone, String organization) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (name != null && !name.isBlank()) user.setName(name);
        if (phone != null && !phone.isBlank()) user.setPhone(phone);
        if (organization != null && !organization.isBlank()) user.setOrganization(organization);
        updateById(user);
    }

    @Override
    public List<User> listAllUsers() {
        return list(new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
    }

    @Override
    @Transactional
    public User createUser(String studentId, String password, String name, String phone,
                           String role, String organization) {
        User exist = findByStudentId(studentId);
        if (exist != null) {
            throw new RuntimeException("该学号已注册");
        }
        User user = new User();
        user.setStudentId(studentId);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);
        user.setPhone(phone);
        user.setRole(role != null && !role.isBlank() ? role : "student");
        user.setTotalHours(BigDecimal.ZERO);
        if (organization != null && !organization.isBlank()) {
            user.setOrganization(organization);
        }
        save(user);
        return user;
    }

    @Override
    public void updateUser(Long userId, String name, String phone, String role, String organization) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (name != null && !name.isBlank()) user.setName(name);
        if (phone != null && !phone.isBlank()) user.setPhone(phone);
        if (role != null && !role.isBlank()) user.setRole(role);
        if (organization != null && !organization.isBlank()) user.setOrganization(organization);
        updateById(user);
    }

    @Override
    public void deleteUser(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            throw new RuntimeException("不能删除管理员账户");
        }
        removeById(userId);
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }
}
