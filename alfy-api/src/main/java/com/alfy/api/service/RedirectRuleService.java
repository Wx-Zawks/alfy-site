package com.alfy.api.service;
import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.RedirectRuleResponse;
import com.alfy.api.dto.RedirectRuleUpsertRequest;
import com.alfy.api.entity.RedirectRule;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.RedirectRuleMapper;
import com.alfy.api.security.AdminPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
@Service @RequiredArgsConstructor
public class RedirectRuleService {
    private final RedirectRuleMapper mapper; private final AdminOperationLogService logs;
    public List<RedirectRuleResponse> list() { return mapper.selectList(new LambdaQueryWrapper<RedirectRule>().orderByAsc(RedirectRule::getSourcePath)).stream().map(this::toResponse).toList(); }
    public RedirectRuleResponse get(Long id) { return toResponse(require(id)); }
    @Transactional public RedirectRuleResponse create(RedirectRuleUpsertRequest r, AdminPrincipal p) { validate(r, null); RedirectRule x = new RedirectRule(); apply(x, r); mapper.insert(x); logs.record(p.id(), "CREATE", "REDIRECT_RULE", x.getId(), "创建 301 跳转 " + x.getSourcePath()); return get(x.getId()); }
    @Transactional public RedirectRuleResponse update(Long id, RedirectRuleUpsertRequest r, AdminPrincipal p) { RedirectRule x = require(id); if (r.version() == null || !r.version().equals(x.getVersion())) throw new BusinessException(ErrorCode.CONFLICT, "301 规则已被其他管理员修改，请刷新后重试"); validate(r, id); apply(x, r); if (mapper.updateById(x) != 1) throw new BusinessException(ErrorCode.CONFLICT, "301 规则已被其他管理员修改"); logs.record(p.id(), "UPDATE", "REDIRECT_RULE", id, "更新 301 跳转 " + x.getSourcePath()); return get(id); }
    @Transactional public void delete(Long id, AdminPrincipal p) { RedirectRule x = require(id); mapper.deleteById(id); logs.record(p.id(), "DELETE", "REDIRECT_RULE", id, "删除 301 跳转 " + x.getSourcePath()); }
    private void validate(RedirectRuleUpsertRequest r, Long current) { String source = r.sourcePath().trim(); if (!source.startsWith("/") || source.startsWith("//") || source.contains("?")) throw new BusinessException(ErrorCode.BAD_REQUEST, "来源路径必须是无查询参数的站内绝对路径"); String target = r.targetUrl().trim(); if (!(target.startsWith("/") || target.startsWith("https://") || target.startsWith("http://"))) throw new BusinessException(ErrorCode.BAD_REQUEST, "目标地址必须是站内路径或 HTTP(S) 地址"); RedirectRule other = mapper.selectOne(new LambdaQueryWrapper<RedirectRule>().eq(RedirectRule::getSourcePath, source)); if (other != null && !other.getId().equals(current)) throw new BusinessException(ErrorCode.CONFLICT, "该来源路径已存在跳转规则"); }
    private void apply(RedirectRule x, RedirectRuleUpsertRequest r) { x.setSourcePath(r.sourcePath().trim()); x.setTargetUrl(r.targetUrl().trim()); x.setEnabled(r.enabled() == null || Boolean.TRUE.equals(r.enabled()) ? 1 : 0); }
    private RedirectRule require(Long id) { RedirectRule x = mapper.selectById(id); if (x == null) throw new BusinessException(ErrorCode.NOT_FOUND, "301 规则不存在"); return x; }
    private RedirectRuleResponse toResponse(RedirectRule x) { return new RedirectRuleResponse(x.getId(), x.getSourcePath(), x.getTargetUrl(), Integer.valueOf(1).equals(x.getEnabled()), x.getVersion(), x.getUpdatedAt()); }
}
