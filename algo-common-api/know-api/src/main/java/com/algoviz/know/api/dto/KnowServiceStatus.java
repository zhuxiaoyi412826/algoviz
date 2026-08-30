package com.algoviz.know.api.dto;

import java.io.Serializable;

/** 服务状态（探活）：模型是否就绪 / Qdrant 是否连通 / 维度 / 集合名 */
public class KnowServiceStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean modelReady;
    private boolean qdrantConnected;
    private int dim;
    private String collectionName;
    private long total;
    private String message;

    public KnowServiceStatus() {
    }

    public boolean isModelReady() {
        return modelReady;
    }

    public void setModelReady(boolean modelReady) {
        this.modelReady = modelReady;
    }

    public boolean isQdrantConnected() {
        return qdrantConnected;
    }

    public void setQdrantConnected(boolean qdrantConnected) {
        this.qdrantConnected = qdrantConnected;
    }

    public int getDim() {
        return dim;
    }

    public void setDim(int dim) {
        this.dim = dim;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
