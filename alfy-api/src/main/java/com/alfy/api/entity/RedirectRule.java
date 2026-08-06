package com.alfy.api.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @TableName("redirect_rule")
public class RedirectRule { @TableId(value = "id", type = IdType.AUTO) private Long id; private String sourcePath; private String targetUrl; private Integer enabled; @Version private Long version; private LocalDateTime createdAt; private LocalDateTime updatedAt; @TableLogic private Integer deleted; }
