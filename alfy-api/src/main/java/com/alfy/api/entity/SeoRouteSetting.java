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
@TableName("seo_route_setting")
public class SeoRouteSetting {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String routeKey;
    private String routePath;
    private String displayName;
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
    private Long ogMediaId;
    private Integer indexEnabled;
    @Version
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
