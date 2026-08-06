package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 博客文章分类。新闻动态为父分类，公司新闻和行业新闻为其子分类。
 */
@Getter
@Setter
@TableName("article_category")
public class ArticleCategory {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
