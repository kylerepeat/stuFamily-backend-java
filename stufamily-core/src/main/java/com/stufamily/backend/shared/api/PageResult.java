package com.stufamily.backend.shared.api;

import java.util.List;

public record PageResult<T>(
    List<T> items,
    long total,
    int pageNo,
    int pageSize,
    long totalPages
) {
    public static <T> PageResult<T> of(List<T> items, long total, int pageNo, int pageSize) {
        long pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(items, total, pageNo, pageSize, pages);
    }
}
