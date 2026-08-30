package com.algoviz.know.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 分页列出向量及 payload（list 每项含 payload 字段 + 可选 "vector" 完整向量数组） */
public class KnowPageResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Map<String, Object>> list = new ArrayList<>();
    private long total;
    private int page;
    private int pageSize;

    public KnowPageResult() {
    }

    public List<Map<String, Object>> getList() {
        return list;
    }

    public void setList(List<Map<String, Object>> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
