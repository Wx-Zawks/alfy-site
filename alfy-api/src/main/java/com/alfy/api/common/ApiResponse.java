package com.alfy.api.common;

/**
 * API 统一返回结构。
 *
 * @param code    业务状态码，0 表示成功
 * @param message 提示信息
 * @param data    返回数据
 */
public record ApiResponse<T>(
        int code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(0, "success", null);
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}