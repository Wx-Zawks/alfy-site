package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.ContentPageResponse;
import com.alfy.api.dto.ContentPageUpsertRequest;
import com.alfy.api.entity.ContentPage;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.ContentPageMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Pattern;

@Service @RequiredArgsConstructor
public class ContentPageService {
    private static final Pattern KEY = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Set<String> STATUS = Set.of("DRAFT", "PUBLISHED", "OFFLINE");
    private final ContentPageMapper mapper; private final AdminOperationLogService logs; private final HtmlSanitizer sanitizer;
    private final ObjectMapper objectMapper;
    public Page<ContentPageResponse> list(String status, String keyword, long page, long size) { if (status != null && !status.isBlank() && !STATUS.contains(status)) throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的单页状态"); Page<ContentPage> result = mapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<ContentPage>().eq(status != null && !status.isBlank(), ContentPage::getStatus, status).and(keyword != null && !keyword.isBlank(), q -> q.like(ContentPage::getTitle, keyword).or().like(ContentPage::getPageKey, keyword)).orderByDesc(ContentPage::getIsFeatured).orderByAsc(ContentPage::getSortOrder).orderByAsc(ContentPage::getId)); Page<ContentPageResponse> out = new Page<>(result.getCurrent(), result.getSize(), result.getTotal()); out.setRecords(result.getRecords().stream().map(this::toResponse).toList()); return out; }
    public ContentPageResponse get(Long id) { return toResponse(require(id)); }
    public ContentPageResponse getPublic(String pageKey) { ContentPage page = mapper.selectOne(new LambdaQueryWrapper<ContentPage>().eq(ContentPage::getPageKey, pageKey).eq(ContentPage::getStatus, "PUBLISHED")); if (page == null) throw new BusinessException(ErrorCode.NOT_FOUND, "页面不存在或尚未发布"); return toResponse(page); }
    @Transactional public ContentPageResponse create(ContentPageUpsertRequest r, AdminPrincipal p) { validate(r, null); ContentPage x = new ContentPage(); apply(x, r); x.setStatus("DRAFT"); mapper.insert(x); logs.record(p.id(), "CREATE", "CONTENT_PAGE", x.getId(), "创建单页草稿 " + x.getPageKey()); return get(x.getId()); }
    @Transactional public ContentPageResponse update(Long id, ContentPageUpsertRequest r, AdminPrincipal p) { ContentPage x = require(id); version(r.version(), x.getVersion()); validate(r, id); apply(x, r); if (mapper.updateById(x) != 1) throw new BusinessException(ErrorCode.CONFLICT, "单页已被其他管理员修改"); logs.record(p.id(), "UPDATE", "CONTENT_PAGE", id, "更新单页 " + x.getPageKey()); return get(id); }
    @Transactional public ContentPageResponse publish(Long id, AdminPrincipal p) { ContentPage x = require(id); if (x.getTitle().isBlank() || x.getSummary() == null || x.getSummary().isBlank()) throw new BusinessException(ErrorCode.BAD_REQUEST, "发布前必须填写标题和摘要"); x.setStatus("PUBLISHED"); if (x.getPublishedAt() == null) x.setPublishedAt(LocalDateTime.now()); mapper.updateById(x); logs.record(p.id(), "PUBLISH", "CONTENT_PAGE", id, "发布单页 " + x.getPageKey()); return get(id); }
    @Transactional public ContentPageResponse offline(Long id, AdminPrincipal p) { ContentPage x = require(id); x.setStatus("OFFLINE"); mapper.updateById(x); logs.record(p.id(), "OFFLINE", "CONTENT_PAGE", id, "下线单页 " + x.getPageKey()); return get(id); }
    @Transactional public void delete(Long id, AdminPrincipal p) { require(id); mapper.deleteById(id); logs.record(p.id(), "DELETE", "CONTENT_PAGE", id, "删除单页"); }
    private void validate(ContentPageUpsertRequest r, Long currentId) { String key = r.pageKey().trim(); if (!KEY.matcher(key).matches()) throw new BusinessException(ErrorCode.BAD_REQUEST, "pageKey 仅支持小写字母、数字和连字符"); ContentPage duplicate = mapper.selectOne(new LambdaQueryWrapper<ContentPage>().eq(ContentPage::getPageKey, key)); if (duplicate != null && !duplicate.getId().equals(currentId)) throw new BusinessException(ErrorCode.CONFLICT, "页面标识已存在"); }
    private void apply(ContentPage x, ContentPageUpsertRequest r) { x.setPageKey(r.pageKey().trim()); x.setTitle(r.title().trim()); x.setCategory(trim(r.category())); x.setSummary(trim(r.summary())); x.setContentHtml(sanitizer.clean(r.contentHtml())); x.setContentJson(write(r.contentData())); x.setCoverMediaId(r.coverMediaId()); x.setIsFeatured(Boolean.TRUE.equals(r.featured()) ? 1 : 0); x.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder()); x.setSeoTitle(trim(r.seoTitle())); x.setSeoDescription(trim(r.seoDescription())); x.setSeoKeywords(trim(r.seoKeywords())); }
    private ContentPage require(Long id) { ContentPage x = mapper.selectById(id); if (x == null) throw new BusinessException(ErrorCode.NOT_FOUND, "单页不存在"); return x; }
    private void version(Long wanted, Long actual) { if (wanted == null || !wanted.equals(actual)) throw new BusinessException(ErrorCode.CONFLICT, "单页已被其他管理员修改，请刷新后重试"); }
    private ContentPageResponse toResponse(ContentPage x) { return new ContentPageResponse(x.getId(), x.getPageKey(), x.getTitle(), x.getCategory(), x.getSummary(), x.getContentHtml(), read(x.getContentJson()), x.getCoverMediaId(), url(x.getCoverMediaId()), Integer.valueOf(1).equals(x.getIsFeatured()), x.getSortOrder(), x.getStatus(), x.getSeoTitle(), x.getSeoDescription(), x.getSeoKeywords(), x.getPublishedAt(), x.getVersion()); }
    private String write(JsonNode value) {
        if (value == null || value.isNull()) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单页结构化内容格式不正确");
        }
    }
    private JsonNode read(String value) {
        if (value == null || value.isBlank()) return objectMapper.createObjectNode();
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "单页结构化内容无法读取");
        }
    }
    private static String trim(String value) { return value == null ? null : value.trim(); } private static String url(Long id) { return id == null ? null : "/api/v1/public/media/" + id; }
}
