package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @TableName("partner")
public class Partner {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String name; private String slug; private String category; private String summary; private Long logoMediaId;
    private String websiteUrl; private Integer isFeatured; private Integer sortOrder; private String status;
    private String seoTitle; private String seoDescription; private String seoKeywords; private LocalDateTime publishedAt;
    private LocalDateTime createdAt; private LocalDateTime updatedAt; @TableLogic private Integer deleted; @Version private Long version;
}
