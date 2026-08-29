package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.AdminCaseCategoryResponse;
import com.alfy.api.dto.AdminCaseCategoryUpsertRequest;
import com.alfy.api.entity.CaseCategory;
import com.alfy.api.entity.CaseProject;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.CaseCategoryMapper;
import com.alfy.api.mapper.CaseProjectMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminCaseCategoryService {
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final CaseCategoryMapper mapper;
    private final CaseProjectMapper caseProjectMapper;
    private final AdminOperationLogService logs;

    public Page<AdminCaseCategoryResponse> list(String keyword, long page, long size) {
        Page<CaseCategory> result = mapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<CaseCategory>()
                        .and(keyword != null && !keyword.isBlank(), query -> query
                                .like(CaseCategory::getName, keyword)
                                .or().like(CaseCategory::getSlug, keyword))
                        .orderByAsc(CaseCategory::getSortOrder)
                        .orderByAsc(CaseCategory::getId));
        Page<AdminCaseCategoryResponse> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::toResponse).toList());
        return response;
    }

    public AdminCaseCategoryResponse get(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public AdminCaseCategoryResponse create(AdminCaseCategoryUpsertRequest request, AdminPrincipal principal) {
        validate(request, null);
        CaseCategory category = new CaseCategory();
        apply(category, request);
        mapper.insert(category);
        logs.record(principal.id(), "CREATE", "CASE_CATEGORY", category.getId(), "创建案例分类");
        return get(category.getId());
    }

    @Transactional
    public AdminCaseCategoryResponse update(Long id, AdminCaseCategoryUpsertRequest request, AdminPrincipal principal) {
        CaseCategory category = require(id);
        assertVersion(request.version(), category.getVersion());
        validate(request, id);
        apply(category, request);
        if (mapper.updateById(category) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "案例分类已被其他管理员修改");
        }
        logs.record(principal.id(), "UPDATE", "CASE_CATEGORY", id, "更新案例分类");
        return get(id);
    }

    @Transactional
    public void delete(Long id, AdminPrincipal principal) {
        require(id);
        if (caseProjectMapper.selectCount(new LambdaQueryWrapper<CaseProject>().eq(CaseProject::getCategoryId, id)) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类下仍有案例，不能删除");
        }
        mapper.deleteById(id);
        logs.record(principal.id(), "DELETE", "CASE_CATEGORY", id, "软删除案例分类");
    }

    private void validate(AdminCaseCategoryUpsertRequest request, Long currentId) {
        String slug = request.slug().trim();
        if (!SLUG.matcher(slug).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "slug 仅支持小写字母、数字和连字符");
        }
        CaseCategory duplicate = mapper.selectOne(new LambdaQueryWrapper<CaseCategory>().eq(CaseCategory::getSlug, slug));
        if (duplicate != null && !duplicate.getId().equals(currentId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "slug 已被其他案例分类使用");
        }
    }

    private void apply(CaseCategory category, AdminCaseCategoryUpsertRequest request) {
        category.setName(request.name().trim());
        category.setSlug(request.slug().trim());
        category.setSummary(request.summary());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setStatus(Boolean.FALSE.equals(request.enabled()) ? 0 : 1);
    }

    private void assertVersion(Long requested, Long actual) {
        if (requested == null || !requested.equals(actual)) {
            throw new BusinessException(ErrorCode.CONFLICT, "案例分类已被其他管理员修改，请刷新后重试");
        }
    }

    private CaseCategory require(Long id) {
        CaseCategory category = mapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "案例分类不存在");
        }
        return category;
    }

    private AdminCaseCategoryResponse toResponse(CaseCategory category) {
        return new AdminCaseCategoryResponse(category.getId(), category.getName(), category.getSlug(),
                category.getSummary(), category.getSortOrder(), Integer.valueOf(1).equals(category.getStatus()),
                category.getVersion());
    }
}
