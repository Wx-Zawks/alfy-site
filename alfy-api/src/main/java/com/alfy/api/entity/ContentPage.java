package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @TableName("content_page")
public class ContentPage {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String pageKey; private String title; private String category; private String summary; private String contentHtml;
    private Long coverMediaId; private Integer isFeatured; private Integer sortOrder; private String status;
    private String seoTitle; private String seoDescription; private String seoKeywords; private LocalDateTime publishedAt;
    private LocalDateTime createdAt; private LocalDateTime updatedAt; @TableLogic private Integer deleted; @Version private Long version;
}
