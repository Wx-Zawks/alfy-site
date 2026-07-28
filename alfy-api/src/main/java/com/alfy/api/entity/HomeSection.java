package com.alfy.api.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @TableName("home_section")
public class HomeSection { @TableId(value = "id", type = IdType.AUTO) private Long id; private String sectionKey; private String label; @TableField(updateStrategy = FieldStrategy.ALWAYS) private String eyebrow; private String title; @TableField(updateStrategy = FieldStrategy.ALWAYS) private String highlightText; @TableField(updateStrategy = FieldStrategy.ALWAYS) private String descriptionText; @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long imageMediaId; @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long mobileMediaId; @TableField(updateStrategy = FieldStrategy.ALWAYS) private String buttonLabel; @TableField(updateStrategy = FieldStrategy.ALWAYS) private String buttonTarget; private Integer enabled; private Integer sortOrder; @Version private Long version; private LocalDateTime createdAt; private LocalDateTime updatedAt; @TableLogic private Integer deleted; }
