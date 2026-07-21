package com.alfy.api.config;

import com.alfy.api.entity.AdminUser;
import com.alfy.api.mapper.AdminUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 通过环境变量安全地创建首个后台账号，不覆盖任何既有账号。 */
@Component
@RequiredArgsConstructor
public class InitialAdminRunner implements ApplicationRunner {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${alfy.admin-bootstrap.username:}")
    private String username;

    @Value("${alfy.admin-bootstrap.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)
                || adminUserMapper.selectCount(null) > 0) {
            return;
        }
        AdminUser admin = new AdminUser();
        admin.setUsername(username.trim());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole("super_admin");
        admin.setStatus(1);
        adminUserMapper.insert(admin);
    }
}
