package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.ActionResponse;
import com.alfy.api.dto.TechnologyPageResponse;
import com.alfy.api.dto.TechnologyPageUpsertRequest;
import com.alfy.api.entity.TechnologyPage;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.TechnologyPageMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TechnologyPageService {
    private static final String PAGE_KEY = "technology";
    private final TechnologyPageMapper technologyPageMapper;
    private final AdminOperationLogService operationLogService;
    private final HtmlSanitizer htmlSanitizer;
    private final ObjectMapper objectMapper;

    public TechnologyPageResponse getAdmin() { return toResponse(require()); }
    public TechnologyPageResponse getPublic() {
        TechnologyPage page = technologyPageMapper.selectOne(new LambdaQueryWrapper<TechnologyPage>().eq(TechnologyPage::getPageKey, PAGE_KEY).eq(TechnologyPage::getStatus, "PUBLISHED"));
        if (page == null) throw new BusinessException(ErrorCode.NOT_FOUND, "技术研发内容尚未发布"); return toResponse(page);
    }
    public TechnologyPageResponse findPublic() {
        TechnologyPage page = technologyPageMapper.selectOne(new LambdaQueryWrapper<TechnologyPage>().eq(TechnologyPage::getPageKey, PAGE_KEY).eq(TechnologyPage::getStatus, "PUBLISHED"));
        return page == null ? null : toResponse(page);
    }

    @Transactional
    public TechnologyPageResponse save(TechnologyPageUpsertRequest request, AdminPrincipal principal) {
        TechnologyPage page = technologyPageMapper.selectOne(new LambdaQueryWrapper<TechnologyPage>().eq(TechnologyPage::getPageKey, PAGE_KEY));
        if (page == null) { page = new TechnologyPage(); page.setPageKey(PAGE_KEY); apply(page, request); page.setStatus("DRAFT"); technologyPageMapper.insert(page); operationLogService.record(principal.id(), "CREATE", "TECHNOLOGY_PAGE", page.getId(), "创建技术研发草稿"); }
        else { if (request.version() == null || !request.version().equals(page.getVersion())) throw new BusinessException(ErrorCode.CONFLICT, "技术研发内容已被其他管理员修改，请刷新后重试"); apply(page, request); if (technologyPageMapper.updateById(page) != 1) throw new BusinessException(ErrorCode.CONFLICT, "技术研发内容已被其他管理员修改"); operationLogService.record(principal.id(), "UPDATE", "TECHNOLOGY_PAGE", page.getId(), "更新技术研发内容"); }
        return getAdmin();
    }

    @Transactional
    public TechnologyPageResponse publish(AdminPrincipal principal) {
        TechnologyPage page = require(); if (page.getTitle() == null || page.getTitle().isBlank() || page.getSummary() == null || page.getSummary().isBlank()) throw new BusinessException(ErrorCode.BAD_REQUEST, "发布前必须填写标题和简介");
        page.setStatus("PUBLISHED"); if (page.getPublishedAt() == null) page.setPublishedAt(LocalDateTime.now()); technologyPageMapper.updateById(page); operationLogService.record(principal.id(), "PUBLISH", "TECHNOLOGY_PAGE", page.getId(), "发布技术研发内容"); return getAdmin();
    }
    @Transactional public TechnologyPageResponse offline(AdminPrincipal principal) { TechnologyPage page = require(); page.setStatus("OFFLINE"); technologyPageMapper.updateById(page); operationLogService.record(principal.id(), "OFFLINE", "TECHNOLOGY_PAGE", page.getId(), "下线技术研发内容"); return getAdmin(); }

    private TechnologyPage require() { TechnologyPage page = technologyPageMapper.selectOne(new LambdaQueryWrapper<TechnologyPage>().eq(TechnologyPage::getPageKey, PAGE_KEY)); if (page == null) throw new BusinessException(ErrorCode.NOT_FOUND, "技术研发内容尚未创建"); return page; }
    private void apply(TechnologyPage page, TechnologyPageUpsertRequest r) { page.setEyebrow(trim(r.eyebrow())); page.setTitle(r.title().trim()); page.setHighlightText(trim(r.highlightText())); page.setSummary(trim(r.summary())); page.setCtaLabel(trim(r.ctaLabel())); page.setCtaTarget(trim(r.ctaTarget())); page.setCapabilityRowsJson(write(r.capabilityRows())); page.setPillarsJson(write(r.pillars())); page.setContentHtml(htmlSanitizer.clean(r.contentHtml())); }
    private TechnologyPageResponse toResponse(TechnologyPage p) { return new TechnologyPageResponse(p.getId(), p.getEyebrow(), p.getTitle(), p.getHighlightText(), p.getSummary(), new ActionResponse(p.getCtaLabel(), p.getCtaTarget()), read(p.getCapabilityRowsJson()), read(p.getPillarsJson()), p.getContentHtml(), p.getStatus(), p.getPublishedAt(), p.getVersion()); }
    private String write(JsonNode node) { if (node == null || node.isNull()) return null; try { return objectMapper.writeValueAsString(node); } catch (JsonProcessingException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "技术结构数据格式不正确"); } }
    private JsonNode read(String json) { if (json == null || json.isBlank()) return objectMapper.createArrayNode(); try { return objectMapper.readTree(json); } catch (JsonProcessingException e) { throw new BusinessException(ErrorCode.INTERNAL_ERROR, "技术结构数据无法读取"); } }
    private static String trim(String value) { return value == null ? null : value.trim(); }
}
