package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.RedirectResolveResponse;
import com.alfy.api.dto.RedirectRuleResponse;
import com.alfy.api.dto.RedirectRuleUpsertRequest;
import com.alfy.api.entity.RedirectRule;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.RedirectRuleMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RedirectRuleService {
    private static final long LOOKUP_CACHE_TTL_MILLIS = 60_000;
    private static final int LOOKUP_CACHE_MAX_ENTRIES = 10_000;

    private final RedirectRuleMapper mapper;
    private final AdminOperationLogService logs;
    private final ConcurrentHashMap<String, CachedRedirect> lookupCache = new ConcurrentHashMap<>();

    public List<RedirectRuleResponse> list() {
        return mapper.selectList(new LambdaQueryWrapper<RedirectRule>()
                        .orderByAsc(RedirectRule::getSourceHost)
                        .orderByAsc(RedirectRule::getSourcePath))
                .stream().map(this::toResponse).toList();
    }

    public RedirectRuleResponse get(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public RedirectRuleResponse create(RedirectRuleUpsertRequest request, AdminPrincipal principal) {
        validate(request, null);
        RedirectRule rule = new RedirectRule();
        apply(rule, request);
        mapper.insert(rule);
        lookupCache.clear();
        logs.record(principal.id(), "CREATE", "REDIRECT_RULE", rule.getId(),
                "创建跳转 " + displaySource(rule));
        return get(rule.getId());
    }

    @Transactional
    public RedirectRuleResponse update(Long id, RedirectRuleUpsertRequest request, AdminPrincipal principal) {
        RedirectRule rule = require(id);
        if (request.version() == null || !request.version().equals(rule.getVersion())) {
            throw new BusinessException(ErrorCode.CONFLICT, "重定向规则已被其他管理员修改，请刷新后重试");
        }
        validate(request, id);
        apply(rule, request);
        if (mapper.updateById(rule) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "重定向规则已被其他管理员修改");
        }
        lookupCache.clear();
        logs.record(principal.id(), "UPDATE", "REDIRECT_RULE", id,
                "更新跳转 " + displaySource(rule));
        return get(id);
    }

    @Transactional
    public void delete(Long id, AdminPrincipal principal) {
        RedirectRule rule = require(id);
        mapper.deleteById(id);
        lookupCache.clear();
        logs.record(principal.id(), "DELETE", "REDIRECT_RULE", id,
                "删除跳转 " + displaySource(rule));
    }

    @Transactional
    public RedirectResolveResponse resolve(String rawHost, String rawPath, String rawQuery) {
        String host = normalizeHost(rawHost);
        String path = normalizeRequestPath(rawPath);
        RedirectRule match = resolveCached(host, path);
        if (match == null) return null;

        String target = appendQuery(match.getTargetUrl(), rawQuery,
                Integer.valueOf(1).equals(match.getPreserveQuery()));
        mapper.update(null, new LambdaUpdateWrapper<RedirectRule>()
                .eq(RedirectRule::getId, match.getId())
                .setSql("hit_count = hit_count + 1")
                .set(RedirectRule::getLastHitAt, LocalDateTime.now()));
        return new RedirectResolveResponse(target, normalizeStatusCode(match.getStatusCode()));
    }

    private RedirectRule resolveCached(String host, String path) {
        String key = host + "\n" + path;
        long now = System.currentTimeMillis();
        CachedRedirect cached = lookupCache.get(key);
        if (cached != null && cached.expiresAt() > now) return cached.rule();

        List<RedirectRule> candidates = mapper.selectList(new LambdaQueryWrapper<RedirectRule>()
                .eq(RedirectRule::getSourcePath, path)
                .eq(RedirectRule::getEnabled, 1)
                .and(query -> query.eq(RedirectRule::getSourceHost, host)
                        .or().eq(RedirectRule::getSourceHost, "")));
        RedirectRule match = candidates.stream()
                .filter(rule -> host.equals(normalizeHost(rule.getSourceHost())))
                .findFirst()
                .orElseGet(() -> candidates.stream()
                        .filter(rule -> normalizeHost(rule.getSourceHost()).isEmpty())
                        .findFirst().orElse(null));
        if (lookupCache.size() >= LOOKUP_CACHE_MAX_ENTRIES) lookupCache.clear();
        lookupCache.put(key, new CachedRedirect(match, now + LOOKUP_CACHE_TTL_MILLIS));
        return match;
    }

    private void validate(RedirectRuleUpsertRequest request, Long currentId) {
        String host = normalizeHost(request.sourceHost());
        String source = normalizeRequestPath(request.sourcePath());
        if (!source.startsWith("/") || source.startsWith("//") || source.contains("?")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "来源路径必须是无查询参数的站内绝对路径");
        }
        String target = request.targetUrl().trim();
        if (!(target.startsWith("/") || target.startsWith("https://") || target.startsWith("http://"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目标地址必须是站内路径或 HTTP(S) 地址");
        }
        if (target.startsWith("//")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目标地址不能使用省略协议的外部地址");
        }
        int statusCode = normalizeStatusCode(request.statusCode());
        if (request.statusCode() != null && statusCode != request.statusCode()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "永久跳转状态码只支持 301 或 308");
        }
        if (target.equals(source)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "来源路径和目标路径不能相同");
        }
        RedirectRule duplicate = mapper.selectOne(new LambdaQueryWrapper<RedirectRule>()
                .eq(RedirectRule::getSourceHost, host)
                .eq(RedirectRule::getSourcePath, source)
                .last("LIMIT 1"));
        if (duplicate != null && !duplicate.getId().equals(currentId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "相同来源主机和路径的跳转规则已存在");
        }
        if (target.startsWith("/")) {
            RedirectRule chained = mapper.selectOne(new LambdaQueryWrapper<RedirectRule>()
                    .eq(RedirectRule::getSourcePath, target)
                    .eq(RedirectRule::getEnabled, 1)
                    .ne(currentId != null, RedirectRule::getId, currentId)
                    .last("LIMIT 1"));
            if (chained != null) {
                throw new BusinessException(ErrorCode.CONFLICT, "目标地址仍是另一条跳转来源，会形成重定向链");
            }
        }
    }

    private void apply(RedirectRule rule, RedirectRuleUpsertRequest request) {
        rule.setSourceHost(normalizeHost(request.sourceHost()));
        rule.setSourcePath(normalizeRequestPath(request.sourcePath()));
        rule.setTargetUrl(request.targetUrl().trim());
        rule.setStatusCode(normalizeStatusCode(request.statusCode()));
        rule.setPreserveQuery(Boolean.TRUE.equals(request.preserveQuery()) ? 1 : 0);
        rule.setNote(trimToNull(request.note()));
        rule.setEnabled(Boolean.FALSE.equals(request.enabled()) ? 0 : 1);
    }

    private RedirectRule require(Long id) {
        RedirectRule rule = mapper.selectById(id);
        if (rule == null) throw new BusinessException(ErrorCode.NOT_FOUND, "重定向规则不存在");
        return rule;
    }

    private RedirectRuleResponse toResponse(RedirectRule rule) {
        return new RedirectRuleResponse(
                rule.getId(), normalizeHost(rule.getSourceHost()), rule.getSourcePath(), rule.getTargetUrl(),
                normalizeStatusCode(rule.getStatusCode()), Integer.valueOf(1).equals(rule.getPreserveQuery()),
                rule.getNote(), Integer.valueOf(1).equals(rule.getEnabled()),
                rule.getHitCount() == null ? 0 : rule.getHitCount(), rule.getLastHitAt(),
                rule.getVersion(), rule.getUpdatedAt()
        );
    }

    private static String displaySource(RedirectRule rule) {
        String host = normalizeHost(rule.getSourceHost());
        return (host.isEmpty() ? "*" : host) + rule.getSourcePath();
    }

    private static int normalizeStatusCode(Integer value) {
        return Integer.valueOf(308).equals(value) ? 308 : 301;
    }

    private static String normalizeHost(String value) {
        if (value == null) return "";
        String host = value.trim().toLowerCase(Locale.ROOT);
        int portIndex = host.indexOf(':');
        return portIndex > 0 ? host.substring(0, portIndex) : host;
    }

    private static String normalizeRequestPath(String value) {
        if (value == null || value.isBlank()) return "/";
        String path = value.trim();
        if (!path.startsWith("/")) return path;
        while (path.contains("//")) path = path.replace("//", "/");
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private static String appendQuery(String target, String rawQuery, boolean preserve) {
        if (!preserve || rawQuery == null || rawQuery.isBlank()) return target;
        String query = rawQuery.startsWith("?") ? rawQuery.substring(1) : rawQuery;
        return target + (target.contains("?") ? "&" : "?") + query;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record CachedRedirect(RedirectRule rule, long expiresAt) { }
}
