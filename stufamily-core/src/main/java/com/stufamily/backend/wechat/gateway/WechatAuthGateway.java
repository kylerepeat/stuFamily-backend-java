package com.stufamily.backend.wechat.gateway;

import com.stufamily.backend.wechat.gateway.dto.WechatSession;

public interface WechatAuthGateway {
    WechatSession code2Session(String code);
}

