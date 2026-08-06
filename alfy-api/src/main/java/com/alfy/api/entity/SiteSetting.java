package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("site_setting")
public class SiteSetting {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String companyName;
    private Long logoMediaId;
    private String address;
    private String servicePhone;
    private String serviceEmail;
    private Long wechatQrMediaId;
    private String icpNumber;
    private String copyrightText;
    private String privacyPolicyUrl;
    @Version private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
