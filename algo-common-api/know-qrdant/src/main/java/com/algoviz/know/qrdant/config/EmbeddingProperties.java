package com.algoviz.know.qrdant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** bge-large-zh-v1.5 ONNX 模型配置 */
@Component
@ConfigurationProperties(prefix = "know.embedding")
public class EmbeddingProperties {

    /** 模型目录：model.onnx + vocab.txt（由 tools/export_onnx.py 一次性生成） */
    private String modelDir = "./model/bge-large-zh-v1.5";

    /** 最大序列长度 */
    private int maxSeqLen = 128;

    /** 检索时附加的 BGE 指令前缀 */
    private String queryPrefix = "为这个句子生成表示以用于检索相关文章：";

    public String getModelDir() {
        return modelDir;
    }

    public void setModelDir(String modelDir) {
        this.modelDir = modelDir;
    }

    public int getMaxSeqLen() {
        return maxSeqLen;
    }

    public void setMaxSeqLen(int maxSeqLen) {
        this.maxSeqLen = maxSeqLen;
    }

    public String getQueryPrefix() {
        return queryPrefix;
    }

    public void setQueryPrefix(String queryPrefix) {
        this.queryPrefix = queryPrefix;
    }
}
