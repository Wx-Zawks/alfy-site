package com.alfy.api.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @TableName("base_facility")
public class BaseFacility {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String name;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String address;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long imageMediaId;
    private Integer sortOrder; private Integer enabled;
    @Version private Long version; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
