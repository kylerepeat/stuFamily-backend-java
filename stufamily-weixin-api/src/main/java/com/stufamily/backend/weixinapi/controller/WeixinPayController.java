package com.stufamily.backend.weixinapi.controller;

import com.stufamily.backend.order.application.command.PayNotifyCommand;
import com.stufamily.backend.order.application.service.OrderApplicationService;
import com.stufamily.backend.shared.api.ApiResponse;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import com.stufamily.backend.wechat.config.WechatProperties;
import com.stufamily.backend.wechat.gateway.WechatPayGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatPayNotifyResult;
import com.stufamily.backend.weixinapi.request.PayNotifyRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weixin/pay")
public class WeixinPayController {
    private static final Logger log = LoggerFactory.getLogger(WeixinPayController.class);
    private static final String WECHAT_NOTIFY_SUCCESS_XML = "<xml><return_code><![CDATA[SUCCESS]]></return_code>"
        + "<return_msg><![CDATA[OK]]></return_msg></xml>";
    private static final String WECHAT_NOTIFY_FAIL_XML = "<xml><return_code><![CDATA[FAIL]]></return_code>"
        + "<return_msg><![CDATA[FAIL]]></return_msg></xml>";

    private final OrderApplicationService orderApplicationService;
    private final WechatPayGateway wechatPayGateway;
    private final WechatProperties wechatProperties;

    public WeixinPayController(OrderApplicationService orderApplicationService, WechatPayGateway wechatPayGateway,
                               WechatProperties wechatProperties) {
        this.orderApplicationService = orderApplicationService;
        this.wechatPayGateway = wechatPayGateway;
        this.wechatProperties = wechatProperties;
    }

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Void> notifyMock(@Valid @RequestBody PayNotifyRequest request) {
        if (!wechatProperties.getPay().isMockNotifyEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "mock pay notify is disabled");
        }
        orderApplicationService.markPaid(new PayNotifyCommand(
            request.outTradeNo(),
            request.transactionId(),
            request.totalAmountCents()
        ));
        return ApiResponse.ok();
    }

    @PostMapping(value = "/notify", consumes = {
        MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.TEXT_PLAIN_VALUE
    }, produces = MediaType.APPLICATION_XML_VALUE)
    public String notify(@RequestBody String notifyPayload) {
        try {
            WechatPayNotifyResult notifyResult = wechatPayGateway.parseOrderNotify(notifyPayload);
            orderApplicationService.markPaid(new PayNotifyCommand(
                notifyResult.outTradeNo(),
                notifyResult.transactionId(),
                notifyResult.totalAmountCents()
            ));
            return WECHAT_NOTIFY_SUCCESS_XML;
        } catch (Exception ex) {
            log.warn("wechat pay notify handle failed", ex);
            return WECHAT_NOTIFY_FAIL_XML;
        }
    }
}
