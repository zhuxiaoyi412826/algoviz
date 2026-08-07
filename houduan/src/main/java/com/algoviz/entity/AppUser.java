package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "前台用户实体")
public class AppUser {
    @Schema(description = "用户ID")
    private String id;
    @Schema(description = "微信OpenID")
    private String openid;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "绑定时间")
    private String bindTime;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "数据结构访问次数")
    private Integer dsVisits;
    @Schema(description = "算法访问次数")
    private Integer algoVisits;
    @Schema(description = "OJ访问次数")
    private Integer ojVisits;
    @Schema(description = "AI对话次数")
    private Integer aiDialogues;
    @Schema(description = "最后访问时间")
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