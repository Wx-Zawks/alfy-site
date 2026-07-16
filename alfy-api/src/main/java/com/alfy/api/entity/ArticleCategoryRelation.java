package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文章的附加分类关联。文章主分类保存在 Article.categoryId，
 * 本表使同一篇文章能够同时出现在多个栏目中。
 */
@Getter
@Setter
@TableName("article_category_relation")
public class ArticleCategoryRelation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long categoryId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
