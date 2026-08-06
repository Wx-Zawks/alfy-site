package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("admin_operation_log")
public class AdminOperationLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String action;
    private String objectType;
    private Long objectId;
    private String detail;
    private LocalDateTime createdAt;
}
