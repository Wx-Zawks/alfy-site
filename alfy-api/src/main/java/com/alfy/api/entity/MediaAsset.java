package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文章图片、视频和附件的统一资源记录。
 */
@Getter
@Setter
@TableName("media_asset")
public class MediaAsset {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String mediaType;
    private String storageKey;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private String sha256;
    private Integer width;
    private Integer height;
    private Integer durationSeconds;
    private String altText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
