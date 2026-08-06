package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("product")
public class Product {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String name;
    private String slug;
    private String summary;
    private String contentHtml;
    private String contentText;
    private Long coverMediaId;
    private String featuresJson;
    private String specificationsJson;
    private String status;
    private Integer isFeatured;
    private Integer sortOrder;
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
    @Version
    private Long version;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
