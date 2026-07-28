package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.AdminUserCreateRequest;
import com.alfy.api.dto.AdminUserResponse;
import com.alfy.api.dto.AdminUserUpdateRequest;
import com.alfy.api.entity.AdminUser;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.AdminUserMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String SUPER_ADMIN = "super_admin";

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminOperationLogService operationLogService;

    public List<AdminUserResponse> list() {
        return adminUserMapper.selectList(new LambdaQueryWrapper<AdminUser>()
                        .orderByAsc(AdminUser::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse create(AdminUserCreateRequest request, AdminPrincipal principal) {
        String username = request.username().trim();
        if (adminUserMapper.selectCount(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username)) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "管理员账号已存在");
        }
        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(1);
        adminUserMapper.insert(user);
        operationLogService.record(principal.id(), "CREATE", "ADMIN_USER", user.getId(),
                "创建管理员：" + username + "，角色：" + request.role());
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse update(Long id, AdminUserUpdateRequest request, AdminPrincipal principal) {
        AdminUser user = require(id);
        boolean enabled = Boolean.TRUE.equals(request.enabled());
        if (id.equals(principal.id())
                && (!enabled || !request.role().equalsIgnoreCase(principal.role()))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能禁用当前账号或修改当前账号角色");
        }
        boolean removesActiveSuperAdmin = SUPER_ADMIN.equalsIgnoreCase(user.getRole())
                && user.getStatus() != null && user.getStatus() == 1
                && (!enabled || !SUPER_ADMIN.equals(request.role()));
        if (removesActiveSuperAdmin && activeSuperAdminCount() <= 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "至少需要保留一个启用的超级管理员");
        }
        user.setRole(request.role());
        user.setStatus(enabled ? 1 : 0);
        adminUserMapper.updateById(user);
        operationLogService.record(principal.id(), "UPDATE", "ADMIN_USER", user.getId(),
                "更新管理员：" + user.getUsername() + "，角色：" + request.role()
                        + "，状态：" + (enabled ? "启用" : "禁用"));
        return toResponse(user);
    }

    private long activeSuperAdminCount() {
        return adminUserMapper.selectCount(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getRole, SUPER_ADMIN)
                .eq(AdminUser::getStatus, 1));
    }

    private AdminUser require(Long id) {
        AdminUser user = adminUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管理员不存在");
        }
        return user;
    }

    private AdminUserResponse toResponse(AdminUser user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getStatus() != null && user.getStatus() == 1,
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
