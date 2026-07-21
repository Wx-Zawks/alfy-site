package com.alfy.api.service;

import com.alfy.api.entity.AdminOperationLog;
import com.alfy.api.mapper.AdminOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 仅记录非敏感摘要，禁止写入密码、令牌和完整询盘内容。 */
@Service
@RequiredArgsConstructor
public class AdminOperationLogService {

    private final AdminOperationLogMapper adminOperationLogMapper;

    public void record(Long operatorId, String action, String objectType, Long objectId, String detail) {
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setObjectType(objectType);
        log.setObjectId(objectId);
        log.setDetail(detail);
        adminOperationLogMapper.insert(log);
    }
}
