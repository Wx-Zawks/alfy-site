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

@Getter
@Setter
@TableName("page_hero")
public class PageHero {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String pageKey;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String eyebrow;
    private String title;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String highlightText;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String summary;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long backgroundMediaId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long mobileBackgroundMediaId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String primaryActionLabel;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String primaryActionTarget;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String secondaryActionLabel;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String secondaryActionTarget;
    private String status;
    private LocalDateTime publishedAt;
    @Version private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
