package com.algoviz.entity;

public class AppUser {
    private String id;
    private String openid;
    private String nickname;
    private String avatar;
    private String bindTime;
    private String status;
    private Integer dsVisits;
    private Integer algoVisits;
    private Integer ojVisits;
    private Integer aiDialogues;
    private String lastVisitTime;

    public AppUser() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getBindTime() { return bindTime; }
    public void setBindTime(String bindTime) { this.bindTime = bindTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getDsVisits() { return dsVisits; }
    public void setDsVisits(Integer dsVisits) { this.dsVisits = dsVisits; }
    public Integer getAlgoVisits() { return algoVisits; }
    public void setAlgoVisits(Integer algoVisits) { this.algoVisits = algoVisits; }
    public Integer getOjVisits() { return ojVisits; }
    public void setOjVisits(Integer ojVisits) { this.ojVisits = ojVisits; }
    public Integer getAiDialogues() { return aiDialogues; }
    public void setAiDialogues(Integer aiDialogues) { this.aiDialogues = aiDialogues; }
    public String getLastVisitTime() { return lastVisitTime; }
    public void setLastVisitTime(String lastVisitTime) { this.lastVisitTime = lastVisitTime; }
}
