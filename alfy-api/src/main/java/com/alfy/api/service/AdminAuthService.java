package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.AdminAuthResponse;
import com.alfy.api.dto.AdminLoginRequest;
import com.alfy.api.dto.AdminProfileResponse;
import com.alfy.api.entity.AdminRefreshToken;
import com.alfy.api.entity.AdminUser;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.AdminRefreshTokenMapper;
import com.alfy.api.mapper.AdminUserMapper;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.security.JwtTokenService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AdminUserMapper adminUserMapper;
    private final AdminRefreshTokenMapper adminRefreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AdminOperationLogService operationLogService;

    @Value("${alfy.security.access-token-ttl}")
    private Duration accessTokenTtl;

    @Value("${alfy.security.refresh-token-ttl}")
    private Duration refreshTokenTtl;

    @Transactional
    public AdminAuthResponse login(AdminLoginRequest request) {
        AdminUser user = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.username()));
        if (user == null || user.getStatus() == null || user.getStatus() != 1
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        user.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(user);
        operationLogService.record(user.getId(), "LOGIN", "ADMIN_USER", user.getId(), "后台登录成功");
        return issueTokens(user);
    }

    @Transactional
    public AdminAuthResponse refresh(String rawRefreshToken) {
        String tokenHash = sha256(rawRefreshToken);
        AdminRefreshToken refreshToken = adminRefreshTokenMapper.selectOne(new LambdaQueryWrapper<AdminRefreshToken>()
                .eq(AdminRefreshToken::getTokenHash, tokenHash));
        if (refreshToken == null || refreshToken.getRevokedAt() != null
                || !refreshToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
        AdminUser user = adminUserMapper.selectById(refreshToken.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "管理员账号不可用");
        }
        refreshToken.setRevokedAt(LocalDateTime.now());
        adminRefreshTokenMapper.updateById(refreshToken);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken, AdminPrincipal principal) {
        AdminRefreshToken refreshToken = adminRefreshTokenMapper.selectOne(new LambdaQueryWrapper<AdminRefreshToken>()
                .eq(AdminRefreshToken::getTokenHash, sha256(rawRefreshToken))
                .eq(AdminRefreshToken::getUserId, principal.id()));
        if (refreshToken != null && refreshToken.getRevokedAt() == null) {
            refreshToken.setRevokedAt(LocalDateTime.now());
            adminRefreshTokenMapper.updateById(refreshToken);
        }
        operationLogService.record(principal.id(), "LOGOUT", "ADMIN_USER", principal.id(), "后台退出登录");
    }

    public AdminProfileResponse currentProfile(AdminPrincipal principal) {
        return new AdminProfileResponse(principal.id(), principal.username(), principal.role());
    }

    private AdminAuthResponse issueTokens(AdminUser user) {
        AdminPrincipal principal = new AdminPrincipal(user.getId(), user.getUsername(), user.getRole());
        String rawRefreshToken = newRefreshToken();
        AdminRefreshToken refreshToken = new AdminRefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(sha256(rawRefreshToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plus(refreshTokenTtl));
        adminRefreshTokenMapper.insert(refreshToken);
        return new AdminAuthResponse(
                jwtTokenService.issueAccessToken(principal), rawRefreshToken, "Bearer", accessTokenTtl.toSeconds(),
                new AdminProfileResponse(user.getId(), user.getUsername(), user.getRole()));
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return BASE64_URL_ENCODER.encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算令牌摘要", exception);
        }
    }

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
}
