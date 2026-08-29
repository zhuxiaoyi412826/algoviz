package com.algoviz.know.api.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * 单条入库项：id + 待向量化文本 + payload（题目元数据）。
 */
public class KnowUpsertItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 唯一 id（建议用算法题 id 字符串） */
    private String id;

    /** 待向量化文本（name + category + description + 复杂度 + 伪代码 拼装后传入） */
    private String text;

    /** payload 元数据：key=字段名, value=字符串（title/category/description/id…） */
    private Map<String, String> payload;

    public KnowUpsertItem() {
    }

    public KnowUpsertItem(String id, String text, Map<String, String> payload) {
        this.id = id;
        this.text = text;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map<String, String> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, String> payload) {
        this.payload = payload;
    }
}
