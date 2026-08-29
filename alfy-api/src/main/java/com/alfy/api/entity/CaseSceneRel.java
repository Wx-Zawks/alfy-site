package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("case_scene_rel")
public class CaseSceneRel {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long caseId;
    private Long sceneId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
