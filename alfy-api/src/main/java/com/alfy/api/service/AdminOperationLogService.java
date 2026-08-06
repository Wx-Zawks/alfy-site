package com.alfy.api.service;

import com.alfy.api.dto.AdminOperationLogResponse;
import com.alfy.api.entity.AdminOperationLog;
import com.alfy.api.entity.AdminUser;
import com.alfy.api.mapper.AdminOperationLogMapper;
import com.alfy.api.mapper.AdminUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 仅记录非敏感摘要，禁止写入密码、令牌和完整询盘内容。 */
@Service
@RequiredArgsConstructor
public class AdminOperationLogService {

    private final AdminOperationLogMapper adminOperationLogMapper;
    private final AdminUserMapper adminUserMapper;

    public void record(Long operatorId, String action, String objectType, Long objectId, String detail) {
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setObjectType(objectType);
        log.setObjectId(objectId);
        log.setDetail(detail);
        adminOperationLogMapper.insert(log);
    }

    public Page<AdminOperationLogResponse> list(String keyword, long pageNumber, long pageSize) {
        LambdaQueryWrapper<AdminOperationLog> query = new LambdaQueryWrapper<AdminOperationLog>()
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(AdminOperationLog::getAction, keyword.trim())
                        .or().like(AdminOperationLog::getObjectType, keyword.trim())
                        .or().like(AdminOperationLog::getDetail, keyword.trim()))
                .orderByDesc(AdminOperationLog::getCreatedAt)
                .orderByDesc(AdminOperationLog::getId);
        Page<AdminOperationLog> page = adminOperationLogMapper.selectPage(
                new Page<>(pageNumber, pageSize), query);
        Set<Long> operatorIds = page.getRecords().stream()
                .map(AdminOperationLog::getOperatorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> names = operatorIds.isEmpty()
                ? Map.of()
                : adminUserMapper.selectBatchIds(operatorIds).stream()
                        .collect(Collectors.toMap(AdminUser::getId, AdminUser::getUsername));
        Page<AdminOperationLogResponse> result = new Page<>(
                page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(item -> new AdminOperationLogResponse(
                        item.getId(),
                        item.getOperatorId(),
                        item.getOperatorId() == null
                                ? "系统"
                                : names.getOrDefault(item.getOperatorId(), "系统"),
                        item.getAction(),
                        item.getObjectType(),
                        item.getObjectId(),
                        item.getDetail(),
                        item.getCreatedAt()
                ))
                .toList());
        return result;
    }
}
