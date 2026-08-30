package com.algoviz.know.api.dto;

import java.io.Serializable;
import java.util.Map;

/** 集合信息（管理页展示：配置 + 实时状态） */
public class KnowCollectionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String collectionName;
    private int dim;
    private String distance;
    private Map<String, Object> config;

    public KnowCollectionInfo() {
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public int getDim() {
        return dim;
    }

    public void setDim(int dim) {
        this.dim = dim;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}
