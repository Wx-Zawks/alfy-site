package com.alfy.api.dto;

/** 公开页面可直接消费的站内或站外跳转。 */
public record ActionResponse(String label, String target) { }
