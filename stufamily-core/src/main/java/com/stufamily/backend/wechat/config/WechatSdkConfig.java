package com.stufamily.backend.wechat.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WechatProperties.class)
public class WechatSdkConfig {

    @Bean
    public WxMaService wxMaService(WechatProperties properties) {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(properties.getMiniapp().getAppId());
        config.setSecret(properties.getMiniapp().getSecret());
        config.setMsgDataFormat(properties.getMiniapp().getMsgDataFormat());
        WxMaServiceImpl service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }

    @Bean
    public WxPayService wxPayService(WechatProperties properties) {
        WxPayConfig config = new WxPayConfig();
        config.setAppId(properties.getPay().getAppId());
        config.setMchId(properties.getPay().getMchId());
        config.setMchKey(properties.getPay().getMchKey());
        WxPayServiceImpl service = new WxPayServiceImpl();
        service.setConfig(config);
        return service;
    }
}

