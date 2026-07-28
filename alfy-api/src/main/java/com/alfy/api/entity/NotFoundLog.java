package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("not_found_log")
public class NotFoundLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String path;
    private String lastReferer;
    private Long hitCount;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
}
