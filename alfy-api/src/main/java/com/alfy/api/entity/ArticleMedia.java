package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文章中的图片、视频和附件关联。
 */
@Getter
@Setter
@TableName("article_media")
public class ArticleMedia {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long mediaId;
    private String usageType;
    private Integer sortOrder;
    private String caption;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
