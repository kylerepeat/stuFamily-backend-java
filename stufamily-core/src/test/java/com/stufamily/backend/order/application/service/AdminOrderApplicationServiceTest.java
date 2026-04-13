package com.stufamily.backend.order.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderMainDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.PaymentRefundDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.PaymentTransactionDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.ServiceReviewDO;
import com.stufamily.backend.order.infrastructure.persistence.mapper.OrderMainMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.PaymentRefundMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.PaymentTransactionMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.ServiceReviewMapper;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.wechat.gateway.WechatPayGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatPayRefundResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AdminOrderApplicationServiceTest {

    @Mock
    private OrderMainMapper orderMainMapper;
    @Mock
    private PaymentTransactionMapper paymentTransactionMapper;
    @Mock
    private PaymentRefundMapper paymentRefundMapper;
    @Mock
    private ServiceReviewMapper serviceReviewMapper;
    @Mock
    private FamilyGroupMapper familyGroupMapper;
    @Mock
    private FamilyMemberCardMapper familyMemberCardMapper;
    @Mock
    private WechatPayGateway wechatPayGateway;

    private AdminOrderApplicationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AdminOrderApplicationService(
            orderMainMapper,
            paymentTransactionMapper,
            paymentRefundMapper,
            serviceReviewMapper,
            familyGroupMapper,
            familyMemberCardMapper,
            wechatPayGateway,
            new ObjectMapper()
        );
    }

    @Test
    void disableFamilyGroupShouldCloseGroupAndCancelMembers() {
        OrderMainDO order = new OrderMainDO();
        order.setId(10L);
        order.setOrderNo("ORD_FAM_1");
        order.setOrderType("FAMILY_CARD");
        when(orderMainMapper.selectOne(any())).thenReturn(order);

        FamilyGroupDO group = new FamilyGroupDO();
        group.setId(20L);
        group.setGroupNo("FG1001");
        group.setStatus("ACTIVE");
        group.setCurrentMembers(2);
        when(familyGroupMapper.selectOne(any())).thenReturn(group);

        FamilyMemberCardDO member1 = new FamilyMemberCardDO();
        member1.setId(30L);
        member1.setGroupId(20L);
        member1.setStatus("ACTIVE");
        FamilyMemberCardDO member2 = new FamilyMemberCardDO();
        member2.setId(31L);
        member2.setGroupId(20L);
        member2.setStatus("CANCELLED");
        when(familyMemberCardMapper.selectList(any())).thenReturn(List.of(member1, member2));
        when(familyMemberCardMapper.updateById(any(FamilyMemberCardDO.class))).thenReturn(1);
        when(familyGroupMapper.updateById(any(FamilyGroupDO.class))).thenReturn(1);

        AdminDisableFamilyGroupResult result = service.disableFamilyGroup(new AdminDisableFamilyGroupCommand("ORD_FAM_1", 99L));

        assertEquals("ORD_FAM_1", result.orderNo());
        assertEquals("FG1001", result.groupNo());
        assertEquals("CLOSED", result.groupStatus());
        assertEquals(2, result.totalMemberCount());
        assertEquals(1, result.disabledMemberCount());
        assertEquals("CLOSED", group.getStatus());
        assertEquals(0, group.getCurrentMembers());
        assertEquals("CANCELLED", member1.getStatus());
        verify(familyGroupMapper).updateById(any(FamilyGroupDO.class));
    }

    @Test
    void disableFamilyGroupShouldFailWhenOrderTypeNotFamilyCard() {
        OrderMainDO order = new OrderMainDO();
        order.setId(11L);
        order.setOrderNo("ORD_VAL_1");
        order.setOrderType("VALUE_ADDED_SERVICE");
        when(orderMainMapper.selectOne(any())).thenReturn(order);

        assertThrows(BusinessException.class,
            () -> service.disableFamilyGroup(new AdminDisableFamilyGroupCommand("ORD_VAL_1", 99L)));
        verify(familyGroupMapper, never()).selectOne(any());
    }

    @Test
    void refundOrderShouldFullRefundAndUpdateOrderAndPaymentStatus() {
        OrderMainDO order = new OrderMainDO();
        order.setId(1L);
        order.setOrderNo("ORD123");
        order.setOrderStatus("PAID");
        order.setPayableAmountCents(10000L);
        when(orderMainMapper.selectOne(any())).thenReturn(order);

        PaymentTransactionDO payment = new PaymentTransactionDO();
        payment.setId(2L);
        payment.setOrderId(1L);
        payment.setOutTradeNo("ORD123");
        payment.setTransactionId("WXTX1");
        payment.setPaymentStatus("SUCCESS");
        when(paymentTransactionMapper.selectOne(any())).thenReturn(payment);

        when(paymentRefundMapper.selectList(any())).thenReturn(List.of());
        when(wechatPayGateway.refundOrder(any())).thenReturn(
            new WechatPayRefundResponse("WXRF1", "SUCCESS", "SUCCESS", "SUCCESS", null, null)
        );
        when(paymentRefundMapper.insert(any(PaymentRefundDO.class))).thenReturn(1);
        when(orderMainMapper.updateById(any(OrderMainDO.class))).thenReturn(1);
        when(paymentTransactionMapper.updateById(any(PaymentTransactionDO.class))).thenReturn(1);

        AdminRefundResult result = service.refundOrder(new AdminRefundCommand("ORD123", 10000L, "full refund", 99L));

        assertEquals("REFUNDED", result.orderStatus());
        assertEquals("REFUNDED", result.paymentStatus());
        assertEquals(0L, result.remainRefundableAmountCents());
        assertEquals("REFUNDED", order.getOrderStatus());
        assertEquals("REFUNDED", payment.getPaymentStatus());
    }

    @Test
    void refundOrderShouldPartialRefundAndKeepPaidStatus() {
        OrderMainDO order = new OrderMainDO();
        order.setId(1L);
        order.setOrderNo("ORD123");
        order.setOrderStatus("PAID");
        order.setPayableAmountCents(10000L);
        when(orderMainMapper.selectOne(any())).thenReturn(order);

        PaymentTransactionDO payment = new PaymentTransactionDO();
        payment.setId(2L);
        payment.setOrderId(1L);
        payment.setOutTradeNo("ORD123");
        payment.setTransactionId("WXTX1");
        payment.setPaymentStatus("SUCCESS");
        when(paymentTransactionMapper.selectOne(any())).thenReturn(payment);

        when(paymentRefundMapper.selectList(any())).thenReturn(List.of());
        when(wechatPayGateway.refundOrder(any())).thenReturn(
            new WechatPayRefundResponse("WXRF2", "SUCCESS", "SUCCESS", "SUCCESS", null, null)
        );
        when(paymentRefundMapper.insert(any(PaymentRefundDO.class))).thenReturn(1);
        when(orderMainMapper.updateById(any(OrderMainDO.class))).thenReturn(1);
        when(paymentTransactionMapper.updateById(any(PaymentTransactionDO.class))).thenReturn(1);

        AdminRefundResult result = service.refundOrder(new AdminRefundCommand("ORD123", 3000L, "partial refund", 99L));

        assertEquals("PAID", result.orderStatus());
        assertEquals("SUCCESS", result.paymentStatus());
        assertEquals(7000L, result.remainRefundableAmountCents());
        assertEquals("PAID", order.getOrderStatus());
        assertEquals("SUCCESS", payment.getPaymentStatus());
    }

    @Test
    void refundOrderShouldFailWhenRefundAmountExceedsRemainAmount() {
        OrderMainDO order = new OrderMainDO();
        order.setId(1L);
        order.setOrderNo("ORD123");
        order.setOrderStatus("PAID");
        order.setPayableAmountCents(10000L);
        when(orderMainMapper.selectOne(any())).thenReturn(order);

        PaymentTransactionDO payment = new PaymentTransactionDO();
        payment.setId(2L);
        payment.setOrderId(1L);
        payment.setPaymentStatus("SUCCESS");
        when(paymentTransactionMapper.selectOne(any())).thenReturn(payment);

        PaymentRefundDO alreadyRefunded = new PaymentRefundDO();
        alreadyRefunded.setRefundStatus("SUCCESS");
        alreadyRefunded.setRefundAmountCents(9500L);
        when(paymentRefundMapper.selectList(any())).thenReturn(List.of(alreadyRefunded));

        assertThrows(BusinessException.class,
            () -> service.refundOrder(new AdminRefundCommand("ORD123", 1000L, "too much", 99L)));
        verify(wechatPayGateway, never()).refundOrder(any());
    }

    @Test
    void getProductReviewByOrderIdShouldReturnReview() {
        OrderMainDO order = new OrderMainDO();
        order.setId(1L);
        order.setOrderNo("ORD1001");
        when(orderMainMapper.selectById(1L)).thenReturn(order);

        ServiceReviewDO review = new ServiceReviewDO();
        review.setId(11L);
        review.setOrderId(1L);
        review.setOrderNo("ORD1001");
        review.setBuyerUserId(100L);
        review.setProductId(200L);
        review.setProductType("FAMILY_CARD");
        review.setStars(5);
        review.setContent("很好");
        review.setCreatedAt(OffsetDateTime.now().minusDays(1));
        review.setUpdatedAt(OffsetDateTime.now());
        when(serviceReviewMapper.selectOne(any())).thenReturn(review);

        AdminProductReviewView result = service.getProductReviewByOrderId(1L);
        assertEquals(11L, result.reviewId());
        assertEquals(1L, result.orderId());
        assertEquals(5, result.stars());
    }

    @Test
    void getProductReviewByOrderIdShouldReturnNullWhenNoReview() {
        OrderMainDO order = new OrderMainDO();
        order.setId(1L);
        when(orderMainMapper.selectById(1L)).thenReturn(order);
        when(serviceReviewMapper.selectOne(any())).thenReturn(null);

        AdminProductReviewView result = service.getProductReviewByOrderId(1L);
        assertNull(result);
    }
}
