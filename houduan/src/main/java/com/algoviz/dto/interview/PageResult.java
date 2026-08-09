package com.algoviz.dto.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 通用分页返回结构 */
@Data
@Schema(description = "分页返回结构")
public class PageResult<T> {
    @Schema(description = "数据列表")
    private List<T> list;
    @Schema(description = "总条数")
    private long total;
    @Schema(description = "当前页码（从1开始）")
    private int page;
    @Schema(description = "每页大小")
    private int pageSize;
    @Schema(description = "总页数")
    private int totalPages;

    public static <T> PageResult<T> of(List<T> list, long total, int page, int pageSize) {
        PageResult<T> r = new PageResult<>();
        r.list = list;
        r.total = total;
        r.page = page;
        r.pageSize = pageSize;
        r.totalPages = pageSize <= 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return r;
    }
}
