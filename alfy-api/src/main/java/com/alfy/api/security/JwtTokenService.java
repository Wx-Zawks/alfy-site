package com.alfy.api.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** 最小化的 HS256 JWT 签发与校验，避免后台认证依赖第三方 SaaS。 */
@Service
public class JwtTokenService {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final ObjectMapper objectMapper;
    private final byte[] signingKey;
    private final Duration accessTokenTtl;

    public JwtTokenService(
            ObjectMapper objectMapper,
            @Value("${alfy.security.jwt-secret}") String jwtSecret,
            @Value("${alfy.security.access-token-ttl}") Duration accessTokenTtl
    ) {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("ALFY_SECURITY_JWT_SECRET 必须至少 32 个字符");
        }
        this.objectMapper = objectMapper;
        this.signingKey = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenTtl = accessTokenTtl;
    }

    public String issueAccessToken(AdminPrincipal principal) {
        Instant now = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", principal.id().toString());
        payload.put("username", principal.username());
        payload.put("role", principal.role());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plus(accessTokenTtl).getEpochSecond());
        return encode(payload);
    }

    public AdminPrincipal parseAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("JWT 格式不正确");
            }
            byte[] expected = hmac(parts[0] + "." + parts[1]);
            byte[] actual = BASE64_URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new IllegalArgumentException("JWT 签名无效");
            }
            Map<String, Object> payload = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(parts[1]), new TypeReference<>() { });
            Number expiresAt = (Number) payload.get("exp");
            if (expiresAt == null || expiresAt.longValue() <= Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("JWT 已过期");
            }
            return new AdminPrincipal(
                    Long.parseLong((String) payload.get("sub")),
                    (String) payload.get("username"),
                    (String) payload.get("role"));
        } catch (Exception exception) {
            throw new IllegalArgumentException("JWT 无效", exception);
        }
    }

    private String encode(Map<String, Object> payload) {
        try {
            String encodedHeader = BASE64_URL_ENCODER.encodeToString(HEADER.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String signingInput = encodedHeader + "." + encodedPayload;
            return signingInput + "." + BASE64_URL_ENCODER.encodeToString(hmac(signingInput));
        } catch (Exception exception) {
            throw new IllegalStateException("无法签发访问令牌", exception);
        }
    }

    private byte[] hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("无法校验访问令牌", exception);
        }
    }
}
