package com.algoviz.entity;

public class Statistics {
    private String id;
    private String date;
    private Integer dau;
    private Integer wau;
    private Integer mau;
    private Integer dsVisits;
    private Integer algoVisits;
    private Integer ojSubmissions;
    private Double ojAcRate;
    private Integer aiDialogues;
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
