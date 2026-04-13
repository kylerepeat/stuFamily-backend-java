package com.stufamily.backend.order.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyGroupDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyMemberCardDO;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyGroupMapper;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyMemberCardMapper;
import com.stufamily.backend.order.application.command.AdminDisableFamilyGroupCommand;
import com.stufamily.backend.order.application.command.AdminRefundCommand;
import com.stufamily.backend.order.application.dto.AdminDisableFamilyGroupResult;
import com.stufamily.backend.order.application.dto.AdminProductReviewView;
import com.stufamily.backend.order.application.dto.AdminRefundResult;
import com.stufamily.backend.order.application.dto.AdminRefundView;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderMainDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.PaymentRefundDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.PaymentTransactionDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.ServiceReviewDO;
import com.stufamily.backend.order.infrastructure.persistence.mapper.OrderMainMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.PaymentRefundMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.PaymentTransactionMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.ServiceReviewMapper;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import com.stufamily.backend.wechat.gateway.WechatPayGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatPayRefundRequest;
import com.stufamily.backend.wechat.gateway.dto.WechatPayRefundResponse;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminOrderApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderApplicationService.class);
    private static final String REFUND_STATUS_SUCCESS = "SUCCESS";
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final OrderMainMapper orderMainMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final PaymentRefundMapper paymentRefundMapper;
    private final ServiceReviewMapper serviceReviewMapper;
    private final FamilyGroupMapper familyGroupMapper;
    private final FamilyMemberCardMapper familyMemberCardMapper;
    private final WechatPayGateway wechatPayGateway;
    private final ObjectMapper objectMapper;

    public AdminOrderApplicationService(OrderMainMapper orderMainMapper,
                                        PaymentTransactionMapper paymentTransactionMapper,
                                        PaymentRefundMapper paymentRefundMapper,
                                        ServiceReviewMapper serviceReviewMapper,
                                        FamilyGroupMapper familyGroupMapper,
                                        FamilyMemberCardMapper familyMemberCardMapper,
                                        WechatPayGateway wechatPayGateway,
                                        ObjectMapper objectMapper) {
        this.orderMainMapper = orderMainMapper;
        this.paymentTransactionMapper = paymentTransactionMapper;
        this.paymentRefundMapper = paymentRefundMapper;
        this.serviceReviewMapper = serviceReviewMapper;
        this.familyGroupMapper = familyGroupMapper;
        this.familyMemberCardMapper = familyMemberCardMapper;
        this.wechatPayGateway = wechatPayGateway;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AdminProductReviewView getProductReviewByOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "orderId is required");
        }
        OrderMainDO order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "order not found");
        }
        ServiceReviewDO review = serviceReviewMapper.selectOne(
            new LambdaQueryWrapper<ServiceReviewDO>()
                .eq(ServiceReviewDO::getOrderId, orderId)
                .last("limit 1")
        );
        if (review == null) {
            return null;
        }
        return new AdminProductReviewView(
            review.getId(),
            review.getOrderId(),
            review.getOrderNo(),
            review.getBuyerUserId(),
            review.getProductId(),
            review.getProductType(),
            review.getStars(),
            review.getContent(),
            review.getUpdatedAt(),
            review.getCreatedAt()
        );
    }

    @Transactional
    public AdminDisableFamilyGroupResult disableFamilyGroup(AdminDisableFamilyGroupCommand command) {
        if (command == null || !StringUtils.hasText(command.orderNo())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "orderNo is required");
        }
        OrderMainDO order = requireOrder(command.orderNo());
        if (!"FAMILY_CARD".equals(order.getOrderType())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "only FAMILY_CARD order supports this operation");
        }

        FamilyGroupDO group = familyGroupMapper.selectOne(
            new LambdaQueryWrapper<FamilyGroupDO>()
                .eq(FamilyGroupDO::getSourceOrderId, order.getId())
                .last("limit 1")
        );
        if (group == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "family group not found");
        }

        OffsetDateTime now = OffsetDateTime.now();
        List<FamilyMemberCardDO> members = familyMemberCardMapper.selectList(
            new LambdaQueryWrapper<FamilyMemberCardDO>().eq(FamilyMemberCardDO::getGroupId, group.getId())
        );
        int disabledCount = 0;
        for (FamilyMemberCardDO member : members) {
            if (!"CANCELLED".equals(member.getStatus())) {
                member.setStatus("CANCELLED");
                member.setCancelledAt(now);
                member.setUpdatedAt(now);
                familyMemberCardMapper.updateById(member);
                disabledCount++;
            }
        }

        group.setStatus("CLOSED");
        group.setCurrentMembers(0);
        group.setUpdatedAt(now);
        familyGroupMapper.updateById(group);
        log.info("Admin disabled family group. orderNo={}, groupNo={}, disabledMemberCount={}, operatorUserId={}",
            order.getOrderNo(), group.getGroupNo(), disabledCount, command.operatorUserId());

        return new AdminDisableFamilyGroupResult(
            order.getOrderNo(),
            group.getGroupNo(),
            group.getStatus(),
            members.size(),
            disabledCount,
            now
        );
    }

    @Transactional
    public AdminRefundResult refundOrder(AdminRefundCommand command) {
        validateCommand(command);
        OrderMainDO order = requireOrder(command.orderNo());
        if (!"PAID".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "order is not paid");
        }

        PaymentTransactionDO payment = requirePayment(order.getId());
        if (!"SUCCESS".equals(payment.getPaymentStatus()) && !"REFUNDED".equals(payment.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "payment status does not support refund");
        }

        long refundedAmountCents = sumSuccessfulRefundAmount(payment.getId());
        long remainRefundableAmountCents = order.getPayableAmountCents() - refundedAmountCents;
        if (remainRefundableAmountCents <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "order has been fully refunded");
        }
        if (command.refundAmountCents() > remainRefundableAmountCents) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "refund amount exceeds refundable amount");
        }

        String refundNo = generateRefundNo();
        log.info("Admin refund request. orderNo={}, refundNo={}, amountCents={}, operatorUserId={}",
            order.getOrderNo(), refundNo, command.refundAmountCents(), command.operatorUserId());

        WechatPayRefundResponse gatewayResult = wechatPayGateway.refundOrder(new WechatPayRefundRequest(
            payment.getOutTradeNo(),
            payment.getTransactionId(),
            refundNo,
            order.getPayableAmountCents(),
            command.refundAmountCents(),
            command.reason()
        ));

        OffsetDateTime now = OffsetDateTime.now();
        PaymentRefundDO paymentRefund = new PaymentRefundDO();
        paymentRefund.setPaymentId(payment.getId());
        paymentRefund.setRefundNo(refundNo);
        paymentRefund.setWechatRefundId(gatewayResult.wechatRefundId());
        paymentRefund.setRefundStatus(StringUtils.hasText(gatewayResult.refundStatus())
            ? gatewayResult.refundStatus() : REFUND_STATUS_SUCCESS);
        paymentRefund.setRefundAmountCents(command.refundAmountCents());
        paymentRefund.setReason(command.reason());
        paymentRefund.setRefundPayload(serializeRefundPayload(command, gatewayResult));
        paymentRefund.setSuccessTime(now);
        paymentRefund.setCreatedAt(now);
        paymentRefund.setUpdatedAt(now);
        paymentRefundMapper.insert(paymentRefund);

        long newRefundedAmountCents = refundedAmountCents + command.refundAmountCents();
        boolean fullRefunded = newRefundedAmountCents >= order.getPayableAmountCents();
        order.setOrderStatus(fullRefunded ? "REFUNDED" : "PAID");
        order.setUpdatedAt(now);
        orderMainMapper.updateById(order);

        payment.setPaymentStatus(fullRefunded ? "REFUNDED" : "SUCCESS");
        payment.setUpdatedAt(now);
        paymentTransactionMapper.updateById(payment);

        long newRemainRefundableAmount = Math.max(0L, order.getPayableAmountCents() - newRefundedAmountCents);
        log.info("Admin refund success. orderNo={}, refundNo={}, fullRefunded={}, remainedAmountCents={}",
            order.getOrderNo(), refundNo, fullRefunded, newRemainRefundableAmount);
        return new AdminRefundResult(
            order.getOrderNo(),
            refundNo,
            paymentRefund.getRefundStatus(),
            paymentRefund.getWechatRefundId(),
            command.refundAmountCents(),
            newRefundedAmountCents,
            newRemainRefundableAmount,
            order.getOrderStatus(),
            payment.getPaymentStatus(),
            now
        );
    }

    @Transactional(readOnly = true)
    public PageResult<AdminRefundView> listOrderRefunds(String orderNo, Integer pageNo, Integer pageSize) {
        OrderMainDO order = requireOrder(orderNo);
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        PaymentTransactionDO payment = paymentTransactionMapper.selectOne(
            new LambdaQueryWrapper<PaymentTransactionDO>()
                .eq(PaymentTransactionDO::getOrderId, order.getId())
                .last("limit 1")
        );
        if (payment == null) {
            return PageResult.of(List.of(), 0, normalizedPageNo, normalizedPageSize);
        }
        long total = paymentRefundMapper.selectCount(
            new LambdaQueryWrapper<PaymentRefundDO>()
                .eq(PaymentRefundDO::getPaymentId, payment.getId())
        );
        List<AdminRefundView> items = paymentRefundMapper.selectList(
                new LambdaQueryWrapper<PaymentRefundDO>()
                    .eq(PaymentRefundDO::getPaymentId, payment.getId())
                    .orderByDesc(PaymentRefundDO::getCreatedAt, PaymentRefundDO::getId)
                    .last("limit " + normalizedPageSize + " offset " + offset))
            .stream()
            .map(refund -> new AdminRefundView(
                refund.getRefundNo(),
                refund.getRefundStatus(),
                refund.getWechatRefundId(),
                refund.getRefundAmountCents(),
                refund.getReason(),
                refund.getSuccessTime(),
                refund.getCreatedAt()
            ))
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    private void validateCommand(AdminRefundCommand command) {
        if (command == null || !StringUtils.hasText(command.orderNo())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "orderNo is required");
        }
        if (command.refundAmountCents() == null || command.refundAmountCents() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "refundAmountCents must be greater than zero");
        }
    }

    private OrderMainDO requireOrder(String orderNo) {
        OrderMainDO order = orderMainMapper.selectOne(
            new LambdaQueryWrapper<OrderMainDO>().eq(OrderMainDO::getOrderNo, orderNo).last("limit 1")
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "order not found");
        }
        return order;
    }

    private PaymentTransactionDO requirePayment(Long orderId) {
        PaymentTransactionDO payment = paymentTransactionMapper.selectOne(
            new LambdaQueryWrapper<PaymentTransactionDO>().eq(PaymentTransactionDO::getOrderId, orderId).last("limit 1")
        );
        if (payment == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "payment transaction not found");
        }
        return payment;
    }

    private long sumSuccessfulRefundAmount(Long paymentId) {
        return paymentRefundMapper.selectList(
                new LambdaQueryWrapper<PaymentRefundDO>()
                    .eq(PaymentRefundDO::getPaymentId, paymentId)
                    .eq(PaymentRefundDO::getRefundStatus, REFUND_STATUS_SUCCESS))
            .stream()
            .map(PaymentRefundDO::getRefundAmountCents)
            .filter(amount -> amount != null && amount > 0)
            .mapToLong(Long::longValue)
            .sum();
    }

    private String serializeRefundPayload(AdminRefundCommand command, WechatPayRefundResponse gatewayResult) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderNo", command.orderNo());
            payload.put("operatorUserId", command.operatorUserId());
            payload.put("returnCode", gatewayResult.returnCode());
            payload.put("resultCode", gatewayResult.resultCode());
            payload.put("errCode", gatewayResult.errCode());
            payload.put("errCodeDesc", gatewayResult.errCodeDesc());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "serialize refund payload failed");
        }
    }

    private String generateRefundNo() {
        return "RFD" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);
    }

    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo <= 0) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
