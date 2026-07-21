package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("product_scene_rel")
public class ProductSceneRel {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long sceneId;
    private Integer sortOrder;
}
