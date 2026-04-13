package com.stufamily.backend.wechat.gateway;

import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateRequest;
import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateResponse;
import com.stufamily.backend.wechat.gateway.dto.WechatPayNotifyResult;
import com.stufamily.backend.wechat.gateway.dto.WechatPayRefundRequest;
import com.stufamily.backend.wechat.gateway.dto.WechatPayRefundResponse;

public interface WechatPayGateway {
    WechatPayCreateResponse createMiniappOrder(WechatPayCreateRequest request);

    WechatPayNotifyResult parseOrderNotify(String notifyPayload);

    WechatPayRefundResponse refundOrder(WechatPayRefundRequest request);
}
