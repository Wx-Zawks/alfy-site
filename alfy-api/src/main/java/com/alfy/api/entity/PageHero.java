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
@TableName("page_hero")
public class PageHero {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String pageKey;
    private String eyebrow;
    private String title;
    private String highlightText;
    private String summary;
    private Long backgroundMediaId;
    private Long mobileBackgroundMediaId;
    private String primaryActionLabel;
    private String primaryActionTarget;
    private String secondaryActionLabel;
    private String secondaryActionTarget;
    private String status;
    private LocalDateTime publishedAt;
    @Version private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
