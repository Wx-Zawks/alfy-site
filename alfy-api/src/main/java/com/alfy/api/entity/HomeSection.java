package com.alfy.api.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @TableName("home_section")
public class HomeSection { @TableId(value = "id", type = IdType.AUTO) private Long id; private String sectionKey; private String label; private String eyebrow; private String title; private String highlightText; private String descriptionText; private Long imageMediaId; private Long mobileMediaId; private String buttonLabel; private String buttonTarget; private Integer enabled; private Integer sortOrder; @Version private Long version; private LocalDateTime createdAt; private LocalDateTime updatedAt; @TableLogic private Integer deleted; }
