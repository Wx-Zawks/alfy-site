package com.alfy.api.security;

/** 已通过后台令牌验证的管理员身份。 */
public record AdminPrincipal(Long id, String username, String role) {
}
