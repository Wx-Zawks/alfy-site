package com.alfy.api.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @TableName("team_member")
public class TeamMember {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String role; private String name;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String bio;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long photoMediaId;
    private Integer sortOrder; private Integer enabled;
    @Version private Long version; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
