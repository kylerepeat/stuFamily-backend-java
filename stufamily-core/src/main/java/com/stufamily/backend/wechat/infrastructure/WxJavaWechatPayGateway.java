package com.stufamily.backend.wechat.infrastructure;

import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderResult;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import com.stufamily.backend.wechat.config.WechatProperties;
import com.stufamily.backend.wechat.gateway.WechatPayGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateRequest;
import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateResponse;
import com.stufamily.backend.wechat.gateway.dto.WechatPayNotifyResult;
import com.stufamily.backend.wechat.gateway.dto.WechatPayRefundRequest;
import com.stufamily.backend.wechat.gateway.dto.WechatPayRefundResponse;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WxJavaWechatPayGateway implements WechatPayGateway {

    private final WxPayService wxPayService;
    private final WechatProperties properties;

    public WxJavaWechatPayGateway(WxPayService wxPayService, WechatProperties properties) {
        this.wxPayService = wxPayService;
        this.properties = properties;
    }

    @Override
    public WechatPayCreateResponse createMiniappOrder(WechatPayCreateRequest request) {
        WxPayUnifiedOrderRequest orderRequest = WxPayUnifiedOrderRequest.newBuilder()
            .outTradeNo(request.outTradeNo())
            .openid(request.openid())
            .body(request.body())
            .totalFee((int) request.totalAmountCents())
            .notifyUrl(request.notifyUrl() != null && !request.notifyUrl().isBlank()
                ? request.notifyUrl()
                : properties.getPay().getNotifyUrl())
            .spbillCreateIp(request.clientIp())
            .tradeType("JSAPI")
            .build();
        try {
            WxPayUnifiedOrderResult result = wxPayService.unifiedOrder(orderRequest);
            return new WechatPayCreateResponse(
                result.getPrepayId(),
                result.getNonceStr(),
                result.getSign(),
                String.valueOf(Instant.now().getEpochSecond())
            );
        } catch (WxPayException ex) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "wechat pay unified order failed");
        }
    }

    @Override
    public WechatPayNotifyResult parseOrderNotify(String notifyPayload) {
        try {
            WxPayOrderNotifyResult notifyResult = wxPayService.parseOrderNotifyResult(notifyPayload);
            String returnCode = normalizeCode(notifyResult.getReturnCode());
            String resultCode = normalizeCode(notifyResult.getResultCode());
            if (!"SUCCESS".equals(returnCode) || !"SUCCESS".equals(resultCode)) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "wechat pay notify failed: " + firstNonBlank(notifyResult.getErrCodeDes(), notifyResult.getErrCode()));
            }
            if (!StringUtils.hasText(notifyResult.getOutTradeNo())
                || !StringUtils.hasText(notifyResult.getTransactionId())
                || notifyResult.getTotalFee() == null
                || notifyResult.getTotalFee() <= 0) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "wechat pay notify data invalid");
            }
            return new WechatPayNotifyResult(
                notifyResult.getOutTradeNo(),
                notifyResult.getTransactionId(),
                notifyResult.getTotalFee().longValue()
            );
        } catch (WxPayException ex) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "wechat pay notify signature verify failed");
        }
    }

    @Override
    public WechatPayRefundResponse refundOrder(WechatPayRefundRequest request) {
        WxPayRefundRequest refundRequest = new WxPayRefundRequest();
        refundRequest.setOutTradeNo(request.outTradeNo());
        if (StringUtils.hasText(request.transactionId())) {
            refundRequest.setTransactionId(request.transactionId());
        }
        refundRequest.setOutRefundNo(request.outRefundNo());
        refundRequest.setTotalFee(toWeChatAmount(request.totalAmountCents()));
        refundRequest.setRefundFee(toWeChatAmount(request.refundAmountCents()));
        refundRequest.setOpUserId(properties.getPay().getMchId());
        if (StringUtils.hasText(request.reason())) {
            refundRequest.setRefundDesc(request.reason());
        }
        try {
            WxPayRefundResult result = wxPayService.refund(refundRequest);
            String returnCode = normalizeCode(result.getReturnCode());
            String resultCode = normalizeCode(result.getResultCode());
            if (!"SUCCESS".equals(returnCode) || !"SUCCESS".equals(resultCode)) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "wechat pay refund failed: " + firstNonBlank(result.getErrCodeDes(), result.getErrCode(), result.getReturnMsg()));
            }
            return new WechatPayRefundResponse(
                result.getRefundId(),
                "SUCCESS",
                returnCode,
                resultCode,
                result.getErrCode(),
                result.getErrCodeDes()
            );
        } catch (WxPayException ex) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "wechat pay refund failed");
        }
    }

    private int toWeChatAmount(Long amountCents) {
        if (amountCents == null || amountCents <= 0 || amountCents > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "amount cents out of range");
        }
        return amountCents.intValue();
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return "";
    }
}
