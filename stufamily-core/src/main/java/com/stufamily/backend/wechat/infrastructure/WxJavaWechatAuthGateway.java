package com.stufamily.backend.wechat.infrastructure;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import com.stufamily.backend.wechat.gateway.WechatAuthGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatSession;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Component;

@Component
public class WxJavaWechatAuthGateway implements WechatAuthGateway {

    private final WxMaService wxMaService;

    public WxJavaWechatAuthGateway(WxMaService wxMaService) {
        this.wxMaService = wxMaService;
    }

    @Override
    public WechatSession code2Session(String code) {
        try {
            WxMaJscode2SessionResult result = wxMaService.jsCode2SessionInfo(code);
            return new WechatSession(result.getOpenid(), result.getUnionid(), result.getSessionKey());
        } catch (WxErrorException ex) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "wechat code2session failed");
        }
    }
}

