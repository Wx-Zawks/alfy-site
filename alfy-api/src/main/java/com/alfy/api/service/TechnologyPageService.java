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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TechnologyPageService {

    private static final String OVERVIEW_KEY = "technology";
    private static final List<String> DETAIL_KEYS = List.of(
            "aerogel-material",
            "aerogel-composite",
            "other"
    );
    private static final Set<String> SUPPORTED_KEYS = Set.of(
            OVERVIEW_KEY,
            "aerogel-material",
            "aerogel-composite",
            "other"
    );

    private final TechnologyPageMapper technologyPageMapper;
    private final AdminOperationLogService operationLogService;
    private final HtmlSanitizer htmlSanitizer;
    private final ObjectMapper objectMapper;

    /** 兼容旧后台接口：无 pageKey 时读写技术总览。 */
    public TechnologyPageResponse getAdmin() {
        return getAdmin(OVERVIEW_KEY);
    }

    public TechnologyPageResponse getAdmin(String pageKey) {
        return toResponse(require(pageKey));
    }

    public List<TechnologyPageResponse> listAdmin() {
        return technologyPageMapper.selectList(new LambdaQueryWrapper<TechnologyPage>()
                        .in(TechnologyPage::getPageKey, SUPPORTED_KEYS)
                        .orderByAsc(TechnologyPage::getSortOrder)
                        .orderByAsc(TechnologyPage::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** 技术总览公开接口，保留原有 URL。 */
    public TechnologyPageResponse getPublic() {
        return getPublic(OVERVIEW_KEY);
    }

    public TechnologyPageResponse getPublic(String pageKey) {
        validatePageKey(pageKey);
        TechnologyPage page = technologyPageMapper.selectOne(new LambdaQueryWrapper<TechnologyPage>()
                .eq(TechnologyPage::getPageKey, pageKey)
                .eq(TechnologyPage::getStatus, "PUBLISHED"));
        if (page == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "技术页面不存在或尚未发布");
        }
        return toResponse(page);
    }

    public List<TechnologyPageResponse> listPublicDetails() {
        return technologyPageMapper.selectList(new LambdaQueryWrapper<TechnologyPage>()
                        .in(TechnologyPage::getPageKey, DETAIL_KEYS)
                        .eq(TechnologyPage::getStatus, "PUBLISHED")
                        .orderByAsc(TechnologyPage::getSortOrder)
                        .orderByAsc(TechnologyPage::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TechnologyPageResponse findPublic() {
        TechnologyPage page = technologyPageMapper.selectOne(new LambdaQueryWrapper<TechnologyPage>()
                .eq(TechnologyPage::getPageKey, OVERVIEW_KEY)
                .eq(TechnologyPage::getStatus, "PUBLISHED"));
        return page == null ? null : toResponse(page);
    }

    @Transactional
    public TechnologyPageResponse save(TechnologyPageUpsertRequest request, AdminPrincipal principal) {
        return save(OVERVIEW_KEY, request, principal);
    }

    @Transactional
    public TechnologyPageResponse save(
            String pageKey,
            TechnologyPageUpsertRequest request,
            AdminPrincipal principal
    ) {
        validatePageKey(pageKey);
        TechnologyPage page = find(pageKey);
        if (page == null) {
            page = new TechnologyPage();
            page.setPageKey(pageKey);
            apply(page, request);
            page.setStatus("DRAFT");
            technologyPageMapper.insert(page);
            operationLogService.record(
                    principal.id(), "CREATE", "TECHNOLOGY_PAGE", page.getId(), "创建技术页面 " + pageKey
            );
        } else {
            if (request.version() == null || !request.version().equals(page.getVersion())) {
                throw new BusinessException(ErrorCode.CONFLICT, "技术页面已被其他管理员修改，请刷新后重试");
            }
            apply(page, request);
            if (technologyPageMapper.updateById(page) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "技术页面已被其他管理员修改，请刷新后重试");
            }
            operationLogService.record(
                    principal.id(), "UPDATE", "TECHNOLOGY_PAGE", page.getId(), "更新技术页面 " + pageKey
            );
        }
        return getAdmin(pageKey);
    }

    @Transactional
    public TechnologyPageResponse publish(AdminPrincipal principal) {
        return publish(OVERVIEW_KEY, principal);
    }

    @Transactional
    public TechnologyPageResponse publish(String pageKey, AdminPrincipal principal) {
        TechnologyPage page = require(pageKey);
        if (page.getTitle() == null || page.getTitle().isBlank()
                || page.getSummary() == null || page.getSummary().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "发布前必须填写标题和简介");
        }
        page.setStatus("PUBLISHED");
        if (page.getPublishedAt() == null) {
            page.setPublishedAt(LocalDateTime.now());
        }
        if (technologyPageMapper.updateById(page) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "技术页面已被其他管理员修改，请刷新后重试");
        }
        operationLogService.record(
                principal.id(), "PUBLISH", "TECHNOLOGY_PAGE", page.getId(), "发布技术页面 " + pageKey
        );
        return getAdmin(pageKey);
    }

    @Transactional
    public TechnologyPageResponse offline(AdminPrincipal principal) {
        return offline(OVERVIEW_KEY, principal);
    }

    @Transactional
    public TechnologyPageResponse offline(String pageKey, AdminPrincipal principal) {
        TechnologyPage page = require(pageKey);
        page.setStatus("OFFLINE");
        if (technologyPageMapper.updateById(page) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "技术页面已被其他管理员修改，请刷新后重试");
        }
        operationLogService.record(
                principal.id(), "OFFLINE", "TECHNOLOGY_PAGE", page.getId(), "下线技术页面 " + pageKey
        );
        return getAdmin(pageKey);
    }

    private TechnologyPage find(String pageKey) {
        return technologyPageMapper.selectOne(new LambdaQueryWrapper<TechnologyPage>()
                .eq(TechnologyPage::getPageKey, pageKey));
    }

    private TechnologyPage require(String pageKey) {
        validatePageKey(pageKey);
        TechnologyPage page = find(pageKey);
        if (page == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "技术页面尚未创建");
        }
        return page;
    }

    private void validatePageKey(String pageKey) {
        if (!SUPPORTED_KEYS.contains(pageKey)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "不支持的技术页面");
        }
    }

    private void apply(TechnologyPage page, TechnologyPageUpsertRequest request) {
        page.setEyebrow(trim(request.eyebrow()));
        page.setTitle(request.title().trim());
        page.setHighlightText(trim(request.highlightText()));
        page.setSummary(trim(request.summary()));
        page.setHeroMediaId(request.heroMediaId());
        page.setCtaLabel(trim(request.ctaLabel()));
        page.setCtaTarget(trim(request.ctaTarget()));
        page.setCapabilityRowsJson(write(request.capabilityRows()));
        page.setPillarsJson(write(request.pillars()));
        page.setContentHtml(htmlSanitizer.clean(request.contentHtml()));
        page.setSeoTitle(trim(request.seoTitle()));
        page.setSeoDescription(trim(request.seoDescription()));
        page.setSeoKeywords(trim(request.seoKeywords()));
        page.setSortOrder(
                request.sortOrder() == null ? defaultSortOrder(page.getPageKey()) : request.sortOrder()
        );
    }

    private TechnologyPageResponse toResponse(TechnologyPage page) {
        return new TechnologyPageResponse(
                page.getId(),
                page.getPageKey(),
                page.getEyebrow(),
                page.getTitle(),
                page.getHighlightText(),
                page.getSummary(),
                page.getHeroMediaId(),
                mediaUrl(page.getHeroMediaId()),
                new ActionResponse(page.getCtaLabel(), page.getCtaTarget()),
                readBlocksWithMediaUrls(page.getCapabilityRowsJson()),
                read(page.getPillarsJson()),
                page.getContentHtml(),
                page.getSeoTitle(),
                page.getSeoDescription(),
                page.getSeoKeywords(),
                page.getSortOrder(),
                page.getStatus(),
                page.getPublishedAt(),
                page.getUpdatedAt(),
                page.getVersion()
        );
    }

    private int defaultSortOrder(String pageKey) {
        if (OVERVIEW_KEY.equals(pageKey)) return 0;
        int index = DETAIL_KEYS.indexOf(pageKey);
        return index < 0 ? 99 : index + 1;
    }

    private String write(JsonNode node) {
        if (node == null || node.isNull()) return null;
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "技术结构数据格式不正确");
        }
    }

    private JsonNode read(String json) {
        if (json == null || json.isBlank()) return objectMapper.createArrayNode();
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "技术结构数据无法读取");
        }
    }

    private JsonNode readBlocksWithMediaUrls(String json) {
        JsonNode blocks = read(json);
        if (!blocks.isArray()) return blocks;
        blocks.forEach(block -> {
            if (!(block instanceof ObjectNode object)) return;
            JsonNode imageMediaId = object.get("imageMediaId");
            if (imageMediaId != null && imageMediaId.canConvertToLong() && imageMediaId.longValue() > 0) {
                object.put("imageUrl", mediaUrl(imageMediaId.longValue()));
            } else {
                object.remove("imageUrl");
            }
        });
        return blocks;
    }

    private static String mediaUrl(Long mediaId) {
        return mediaId == null ? null : "/api/v1/public/media/" + mediaId;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
