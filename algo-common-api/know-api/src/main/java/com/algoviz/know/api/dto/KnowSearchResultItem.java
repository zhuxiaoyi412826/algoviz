package com.algoviz.know.api.dto;

import java.io.Serializable;
import java.util.Map;

/** 命中项：id + 相似度 + payload 元数据 */
public class KnowSearchResultItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private double score;
    private Map<String, String> payload;

    public KnowSearchResultItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Map<String, String> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, String> payload) {
        this.payload = payload;
    }
}
