package com.algoviz.entity;

import java.util.List;

/**
 * 通用分页结果类
 * @param <T> 实体类型
 */
public class PageResult<T> {
    
    private List<T> list;      // 当前页数据
    private int total;         // 总记录数
    private int page;          // 当前页码（从1开始）
    private int size;          // 每页大小
    private int totalPages;    // 总页数

    public PageResult() {
    }

    public PageResult(List<T> list, int total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = (int) Math.ceil((double) total / size);
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrev() {
        return page > 1;
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return page < totalPages;
    }
}
