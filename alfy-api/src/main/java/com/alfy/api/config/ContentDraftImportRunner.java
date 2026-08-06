package com.alfy.api.config;

import com.alfy.api.service.ContentDraftImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 仅在显式开启配置时执行草稿导入，避免日常启动服务时意外写入内容数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "alfy.content-import", name = "enabled", havingValue = "true")
public class ContentDraftImportRunner implements ApplicationRunner {

    private final ContentDraftImportService contentDraftImportService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ContentDraftImportService.ImportSummary summary = contentDraftImportService.importDraftPackage();
        log.info("草稿导入任务结果：{}", summary);
    }
}
