package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统计实体")
public class Statistics {
    @Schema(description = "主键ID")
    private String id;
    @Schema(description = "日期")
    private String date;
    @Schema(description = "日活跃用户数")
    private Integer dau;
    @Schema(description = "周活跃用户数")
    private Integer wau;
    @Schema(description = "月活跃用户数")
    private Integer mau;
    @Schema(description = "数据结构访问量")
    private Integer dsVisits;
    @Schema(description = "算法访问量")
    private Integer algoVisits;
    @Schema(description = "OJ提交数")
    private Integer ojSubmissions;
    @Schema(description = "OJ通过率")
    private Double ojAcRate;
    @Schema(description = "AI对话次数")
    private Integer aiDialogues;
    @Schema(description = "创建时间")
    private String createdAt;

    public Statistics() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Integer getDau() { return dau; }
    public void setDau(Integer dau) { this.dau = dau; }
    public Integer getWau() { return wau; }
    public void setWau(Integer wau) { this.wau = wau; }
    public Integer getMau() { return mau; }
    public void setMau(Integer mau) { this.mau = mau; }
    public Integer getDsVisits() { return dsVisits; }
    public void setDsVisits(Integer dsVisits) { this.dsVisits = dsVisits; }
    public Integer getAlgoVisits() { return algoVisits; }
    public void setAlgoVisits(Integer algoVisits) { this.algoVisits = algoVisits; }
    public Integer getOjSubmissions() { return ojSubmissions; }
    public void setOjSubmissions(Integer ojSubmissions) { this.ojSubmissions = ojSubmissions; }
    public Double getOjAcRate() { return ojAcRate; }
    public void setOjAcRate(Double ojAcRate) { this.ojAcRate = ojAcRate; }
    public Integer getAiDialogues() { return aiDialogues; }
    public void setAiDialogues(Integer aiDialogues) { this.aiDialogues = aiDialogues; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}