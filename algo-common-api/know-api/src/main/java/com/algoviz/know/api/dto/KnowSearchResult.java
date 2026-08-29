package com.algoviz.know.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 语义检索结果 */
public class KnowSearchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<KnowSearchResultItem> items = new ArrayList<>();
    private int total;
    private long tookMs;

    public KnowSearchResult() {
    }

    public List<KnowSearchResultItem> getItems() {
        return items;
    }

    public void setItems(List<KnowSearchResultItem> items) {
        this.items = items;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public long getTookMs() {
        return tookMs;
    }

    public void setTookMs(long tookMs) {
        this.tookMs = tookMs;
    }
}
