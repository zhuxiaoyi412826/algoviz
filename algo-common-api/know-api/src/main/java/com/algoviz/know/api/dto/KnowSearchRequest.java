package com.algoviz.know.api.dto;

import java.io.Serializable;

/** 语义检索请求 */
public class KnowSearchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 查询文本（用户输入） */
    private String query;

    /** 返回条数，默认 10 */
    private int topK = 10;

    /** 可选：按分类过滤（category payload 精确匹配），空/ null 不过滤 */
    private String category;

    public KnowSearchRequest() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
