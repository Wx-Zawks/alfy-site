package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("inquiry")
public class Inquiry {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String inquiryNo;
    private String name;
    private String company;
    private String phone;
    private String email;
    private String inquiryType;
    private String message;
    private Long productId;
    private String sourceUrl;
    private String utmJson;
    private Integer privacyAccepted;
    private String status;
    private String adminNote;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
