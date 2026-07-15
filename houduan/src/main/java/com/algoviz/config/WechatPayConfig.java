package com.algoviz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayConfig {
    
    /**
     * 公众号ID/小程序ID
     */
    private String appid;
    
    /**
     * 商户号
     */
    private String mchId;
    
    /**
     * 商户简称
     */
    private String mchShortname;
    
    /**
     * API V3密钥
     */
    private String apiV3Key;
    
    /**
     * 商户证书路径
     */
    private String certPath;
    
    /**
     * 商户私钥路径
     */
    private String privateKeyPath;
    
    /**
     * 商户证书序列号
     */
    private String certSerialNo;
    
    /**
     * 通知地址
     */
    private String notifyUrl;
    
    public String getAppid() {
        return appid;
    }
    
    public void setAppid(String appid) {
        this.appid = appid;
    }
    
    public String getMchId() {
        return mchId;
    }
    
    public void setMchId(String mchId) {
        this.mchId = mchId;
    }
    
    public String getMchShortname() {
        return mchShortname;
    }
    
    public void setMchShortname(String mchShortname) {
        this.mchShortname = mchShortname;
    }
    
    public String getApiV3Key() {
        return apiV3Key;
    }
    
    public void setApiV3Key(String apiV3Key) {
        this.apiV3Key = apiV3Key;
    }
    
    public String getCertPath() {
        return certPath;
    }
    
    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }
    
    public String getPrivateKeyPath() {
        return privateKeyPath;
    }
    
    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }
    
    public String getCertSerialNo() {
        return certSerialNo;
    }
    
    public void setCertSerialNo(String certSerialNo) {
        this.certSerialNo = certSerialNo;
    }
    
    public String getNotifyUrl() {
        return notifyUrl;
    }
    
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }
}
