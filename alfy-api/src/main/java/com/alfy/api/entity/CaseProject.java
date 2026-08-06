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
@TableName("case_project")
public class CaseProject {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long sceneId;
    private String title;
    private String slug;
    private String clientName;
    private String location;
    private String summary;
    private String background;
    private String customerNeed;
    private String solution;
    private String implementation;
    private String resultSummary;
    private String contentHtml;
    private Long coverMediaId;
    private Integer isFeatured;
    private Integer sortOrder;
    private String status;
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
