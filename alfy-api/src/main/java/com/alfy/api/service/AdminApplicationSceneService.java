package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.AdminApplicationSceneResponse;
import com.alfy.api.dto.AdminApplicationSceneUpsertRequest;
import com.alfy.api.entity.ApplicationScene;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.ApplicationSceneMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminApplicationSceneService {
    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "PUBLISHED", "OFFLINE");

    private final ApplicationSceneMapper applicationSceneMapper;
    private final AdminOperationLogService operationLogService;
    private final HtmlSanitizer htmlSanitizer;

    public Page<AdminApplicationSceneResponse> list(String status, String keyword, long page, long size) {
        if (status != null && !status.isBlank() && !VALID_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的场景状态");
        }
        Page<ApplicationScene> result = applicationSceneMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ApplicationScene>().eq(status != null && !status.isBlank(), ApplicationScene::getStatus, status)
                        .and(keyword != null && !keyword.isBlank(), q -> q.like(ApplicationScene::getName, keyword)
                                .or().like(ApplicationScene::getSlug, keyword))
                        .orderByAsc(ApplicationScene::getSortOrder).orderByDesc(ApplicationScene::getUpdatedAt));
        Page<AdminApplicationSceneResponse> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::toResponse).toList());
        return response;
    }

    public AdminApplicationSceneResponse get(Long id) { return toResponse(require(id)); }

    @Transactional
    public AdminApplicationSceneResponse create(AdminApplicationSceneUpsertRequest request, AdminPrincipal principal) {
        validate(request, null);
        ApplicationScene scene = new ApplicationScene();
        apply(scene, request);
        scene.setStatus("DRAFT");
        applicationSceneMapper.insert(scene);
        operationLogService.record(principal.id(), "CREATE", "APPLICATION_SCENE", scene.getId(), "创建应用场景草稿");
        return get(scene.getId());
    }

    @Transactional
    public AdminApplicationSceneResponse update(Long id, AdminApplicationSceneUpsertRequest request, AdminPrincipal principal) {
        ApplicationScene scene = require(id);
        assertVersion(request.version(), scene.getVersion());
        validate(request, id);
        apply(scene, request);
        if (applicationSceneMapper.updateById(scene) != 1) throw new BusinessException(ErrorCode.CONFLICT, "场景已被其他管理员修改");
        operationLogService.record(principal.id(), "UPDATE", "APPLICATION_SCENE", id, "更新应用场景");
        return get(id);
    }

    @Transactional
    public AdminApplicationSceneResponse publish(Long id, AdminPrincipal principal) {
        ApplicationScene scene = require(id);
        if (scene.getName() == null || scene.getName().isBlank() || scene.getSlug() == null || scene.getSlug().isBlank()
                || scene.getSummary() == null || scene.getSummary().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "发布前必须填写名称、slug 和摘要");
        }
        scene.setStatus("PUBLISHED");
        if (scene.getPublishedAt() == null) scene.setPublishedAt(LocalDateTime.now());
        applicationSceneMapper.updateById(scene);
        operationLogService.record(principal.id(), "PUBLISH", "APPLICATION_SCENE", id, "发布应用场景");
        return get(id);
    }

    @Transactional public AdminApplicationSceneResponse offline(Long id, AdminPrincipal principal) {
        ApplicationScene scene = require(id); scene.setStatus("OFFLINE"); applicationSceneMapper.updateById(scene);
        operationLogService.record(principal.id(), "OFFLINE", "APPLICATION_SCENE", id, "下线应用场景"); return get(id);
    }
    @Transactional public void delete(Long id, AdminPrincipal principal) {
        require(id); applicationSceneMapper.deleteById(id);
        operationLogService.record(principal.id(), "DELETE", "APPLICATION_SCENE", id, "软删除应用场景");
    }

    private void validate(AdminApplicationSceneUpsertRequest request, Long currentId) {
        String slug = request.slug().trim();
        if (!SLUG_PATTERN.matcher(slug).matches()) throw new BusinessException(ErrorCode.BAD_REQUEST, "slug 仅支持小写字母、数字和连字符");
        ApplicationScene duplicate = applicationSceneMapper.selectOne(new LambdaQueryWrapper<ApplicationScene>().eq(ApplicationScene::getSlug, slug));
        if (duplicate != null && !duplicate.getId().equals(currentId)) throw new BusinessException(ErrorCode.CONFLICT, "slug 已被其他场景使用");
    }
    private void apply(ApplicationScene scene, AdminApplicationSceneUpsertRequest r) {
        scene.setName(r.name().trim()); scene.setSlug(r.slug().trim()); scene.setSlogan(r.slogan()); scene.setSummary(r.summary());
        scene.setPainPoint(r.painPoint()); scene.setSolution(r.solution()); scene.setContentHtml(htmlSanitizer.clean(r.contentHtml()));
        scene.setCoverMediaId(r.coverMediaId()); scene.setIsFeatured(Boolean.TRUE.equals(r.featured()) ? 1 : 0);
        scene.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder()); scene.setSeoTitle(r.seoTitle());
        scene.setSeoDescription(r.seoDescription()); scene.setSeoKeywords(r.seoKeywords());
    }
    private void assertVersion(Long requested, Long actual) { if (requested == null || !requested.equals(actual)) throw new BusinessException(ErrorCode.CONFLICT, "场景已被其他管理员修改，请刷新后重试"); }
    private ApplicationScene require(Long id) { ApplicationScene scene = applicationSceneMapper.selectById(id); if (scene == null) throw new BusinessException(ErrorCode.NOT_FOUND, "应用场景不存在"); return scene; }
    private AdminApplicationSceneResponse toResponse(ApplicationScene s) { return new AdminApplicationSceneResponse(s.getId(), s.getName(), s.getSlug(), s.getSlogan(), s.getSummary(), s.getPainPoint(), s.getSolution(), s.getContentHtml(), s.getCoverMediaId(), Integer.valueOf(1).equals(s.getIsFeatured()), s.getSortOrder(), s.getStatus(), s.getSeoTitle(), s.getSeoDescription(), s.getSeoKeywords(), s.getPublishedAt(), s.getVersion()); }
}
