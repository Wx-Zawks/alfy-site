package com.alfy.api.service;

import com.alfy.api.dto.NotFoundLogResponse;
import com.alfy.api.dto.NotFoundReportRequest;
import com.alfy.api.entity.NotFoundLog;
import com.alfy.api.mapper.NotFoundLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotFoundLogService {

    private final NotFoundLogMapper notFoundLogMapper;

    @Transactional
    public void record(NotFoundReportRequest request) {
        String path = request.path().trim();
        NotFoundLog log = notFoundLogMapper.selectOne(new LambdaQueryWrapper<NotFoundLog>()
                .eq(NotFoundLog::getPath, path));
        if (log == null) {
            log = new NotFoundLog();
            log.setPath(path);
            log.setLastReferer(trim(request.referer()));
            log.setHitCount(1L);
            notFoundLogMapper.insert(log);
            return;
        }
        log.setLastReferer(trim(request.referer()));
        log.setHitCount((log.getHitCount() == null ? 0 : log.getHitCount()) + 1);
        log.setLastSeenAt(LocalDateTime.now());
        notFoundLogMapper.updateById(log);
    }

    public List<NotFoundLogResponse> list() {
        return notFoundLogMapper.selectList(new LambdaQueryWrapper<NotFoundLog>()
                        .orderByDesc(NotFoundLog::getHitCount)
                        .orderByDesc(NotFoundLog::getLastSeenAt))
                .stream()
                .map(item -> new NotFoundLogResponse(
                        item.getId(),
                        item.getPath(),
                        item.getLastReferer(),
                        item.getHitCount() == null ? 0 : item.getHitCount(),
                        item.getFirstSeenAt(),
                        item.getLastSeenAt()
                ))
                .toList();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
