package com.alfy.api.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 列表分页数据结构。
 *
 * @param records 当前页记录
 * @param total   总记录数
 * @param page    当前页码，从 1 开始
 * @param size    每页记录数
 */
public record PageResponse<T>(
        List<T> records,
        long total,
        long page,
        long size
) {

    public static <T> PageResponse<T> from(IPage<T> page) {
        return new PageResponse<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
