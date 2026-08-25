package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 统一博客文章实体。不同内容类型仅通过分类区分。
 */
@Getter
@Setter
@TableName("article")
public class Article {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 默认展示与编辑使用的主分类；其他分类见 ArticleCategoryRelation。 */
    private Long categoryId;
    private String title;
    private String slug;
    private String summary;
    private String contentHtml;
    private String contentText;
    private Long coverMediaId;
    private String authorName;
    private String sourceUrl;
    private String sourceFile;
    private String contentHash;
    private LocalDateTime sourcePublishedAt;
    private LocalDateTime publishedAt;
    private String status;
    private Integer sortOrder;
    private Integer isFeatured;
    /** 首页展示关闭时必须把已有展示位显式更新为 NULL。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String homeSlot;
    private Integer homeSortOrder;
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
    @Version
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
