package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DOCX 预检和实际导入的可追溯记录。
 */
@Getter
@Setter
@TableName("content_import_record")
public class ContentImportRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private String categoryCode;
    private String sourceFile;
    private String sourceUrl;
    private String sourceHash;
    private String contentHash;
    private String importStatus;
    private String warningMessage;
    private LocalDateTime importedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
