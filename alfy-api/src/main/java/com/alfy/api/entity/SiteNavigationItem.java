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
@TableName("site_navigation_item")
public class SiteNavigationItem {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String navigationArea;
    private Long parentId;
    private String label;
    private String target;
    private Integer sortOrder;
    private Integer highlighted;
    private Integer enabled;
    @Version private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
