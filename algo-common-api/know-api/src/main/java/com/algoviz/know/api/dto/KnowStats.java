package com.algoviz.know.api.dto;

import java.io.Serializable;

/** 集合统计 */
public class KnowStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private long total;
    private int dim;
    private String distance;
    private String collectionName;
    private String status;

    public KnowStats() {
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
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

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
