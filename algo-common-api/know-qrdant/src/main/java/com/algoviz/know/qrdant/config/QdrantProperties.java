package com.algoviz.know.qrdant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Qdrant 连接与集合配置 */
@Component
@ConfigurationProperties(prefix = "know.qdrant")
public class QdrantProperties {

    private String host = "127.0.0.1";
    private int port = 6333;
    private String collection = "algorithm_knowledge";
    private int dim = 1024;
    private String distance = "Cosine";
    private int hnswM = 16;
    private int hnswEfConstruct = 100;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
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

    public int getHnswM() {
        return hnswM;
    }

    public void setHnswM(int hnswM) {
        this.hnswM = hnswM;
    }

    public int getHnswEfConstruct() {
        return hnswEfConstruct;
    }

    public void setHnswEfConstruct(int hnswEfConstruct) {
        this.hnswEfConstruct = hnswEfConstruct;
    }

    public String baseUrl() {
        return "http://" + host + ":" + port;
    }
}
