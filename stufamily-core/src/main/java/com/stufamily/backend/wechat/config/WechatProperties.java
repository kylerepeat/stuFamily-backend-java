package com.stufamily.backend.wechat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.wechat")
public class WechatProperties {
    private Miniapp miniapp = new Miniapp();
    private Pay pay = new Pay();

    public Miniapp getMiniapp() {
        return miniapp;
    }

    public void setMiniapp(Miniapp miniapp) {
        this.miniapp = miniapp;
    }

    public Pay getPay() {
        return pay;
    }

    public void setPay(Pay pay) {
        this.pay = pay;
    }

    public static class Miniapp {
        private String appId = "";
        private String secret = "";
        private String msgDataFormat = "JSON";

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getMsgDataFormat() {
            return msgDataFormat;
        }

        public void setMsgDataFormat(String msgDataFormat) {
            this.msgDataFormat = msgDataFormat;
        }
    }

    public static class Pay {
        private String appId = "";
        private String mchId = "";
        private String mchKey = "";
        private String notifyUrl = "";
        private boolean mockCreateOrderEnabled = false;
        private boolean mockNotifyEnabled = false;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getMchId() {
            return mchId;
        }

        public void setMchId(String mchId) {
            this.mchId = mchId;
        }

        public String getMchKey() {
            return mchKey;
        }

        public void setMchKey(String mchKey) {
            this.mchKey = mchKey;
        }

        public String getNotifyUrl() {
            return notifyUrl;
        }

        public void setNotifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
        }

        public boolean isMockCreateOrderEnabled() {
            return mockCreateOrderEnabled;
        }

        public void setMockCreateOrderEnabled(boolean mockCreateOrderEnabled) {
            this.mockCreateOrderEnabled = mockCreateOrderEnabled;
        }

        public boolean isMockNotifyEnabled() {
            return mockNotifyEnabled;
        }

        public void setMockNotifyEnabled(boolean mockNotifyEnabled) {
            this.mockNotifyEnabled = mockNotifyEnabled;
        }
    }
}
