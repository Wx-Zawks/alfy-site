package com.alfy.api.service;
import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.BaseFacilityResponse;
import com.alfy.api.dto.BaseFacilityUpsertRequest;
import com.alfy.api.entity.BaseFacility;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.BaseFacilityMapper;
import com.alfy.api.security.AdminPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
@Service @RequiredArgsConstructor
public class BaseFacilityService {
    private final BaseFacilityMapper mapper; private final AdminOperationLogService logs;
    public List<BaseFacilityResponse> list(boolean publicOnly) { return mapper.selectList(new LambdaQueryWrapper<BaseFacility>().eq(publicOnly, BaseFacility::getEnabled, 1).orderByAsc(BaseFacility::getSortOrder).orderByAsc(BaseFacility::getId)).stream().map(this::toResponse).toList(); }
    public BaseFacilityResponse get(Long id) { return toResponse(require(id)); }
    @Transactional public BaseFacilityResponse create(BaseFacilityUpsertRequest r, AdminPrincipal p) { BaseFacility x = new BaseFacility(); apply(x, r); mapper.insert(x); logs.record(p.id(), "CREATE", "BASE_FACILITY", x.getId(), "创建产业布局 " + x.getName()); return get(x.getId()); }
    @Transactional public BaseFacilityResponse update(Long id, BaseFacilityUpsertRequest r, AdminPrincipal p) { BaseFacility x = require(id); if (r.version() == null || !r.version().equals(x.getVersion())) throw new BusinessException(ErrorCode.CONFLICT, "产业布局已被其他管理员修改，请刷新后重试"); apply(x, r); if (mapper.updateById(x) != 1) throw new BusinessException(ErrorCode.CONFLICT, "产业布局已被其他管理员修改"); logs.record(p.id(), "UPDATE", "BASE_FACILITY", id, "更新产业布局 " + x.getName()); return get(id); }
    @Transactional public void delete(Long id, AdminPrincipal p) { BaseFacility x = require(id); mapper.deleteById(id); logs.record(p.id(), "DELETE", "BASE_FACILITY", id, "删除产业布局 " + x.getName()); }
    private void apply(BaseFacility x, BaseFacilityUpsertRequest r) { x.setName(r.name().trim()); x.setAddress(trim(r.address())); x.setImageMediaId(r.imageMediaId()); x.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder()); x.setEnabled(r.enabled() == null || Boolean.TRUE.equals(r.enabled()) ? 1 : 0); }
    private BaseFacility require(Long id) { BaseFacility x = mapper.selectById(id); if (x == null) throw new BusinessException(ErrorCode.NOT_FOUND, "产业布局不存在"); return x; }
    private BaseFacilityResponse toResponse(BaseFacility x) { return new BaseFacilityResponse(x.getId(), x.getName(), x.getAddress(), x.getImageMediaId(), url(x.getImageMediaId()), x.getSortOrder(), Integer.valueOf(1).equals(x.getEnabled()), x.getVersion(), x.getUpdatedAt()); }
    private static String trim(String value) { return value == null ? null : value.trim(); }
    private static String url(Long id) { return id == null ? null : "/api/v1/public/media/" + id; }
}
