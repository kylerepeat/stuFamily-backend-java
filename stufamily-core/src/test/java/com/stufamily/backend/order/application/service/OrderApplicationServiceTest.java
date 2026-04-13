package com.stufamily.backend.order.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyGroupDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyMemberCardDO;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyGroupMapper;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyMemberCardMapper;
import com.stufamily.backend.order.application.command.CreateOrderCommand;
import com.stufamily.backend.order.application.command.PayNotifyCommand;
import com.stufamily.backend.order.application.command.SubmitServiceReviewCommand;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderItemDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderMainDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderPurchasedProductRowDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.PaymentTransactionDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.ServiceReviewDO;
import com.stufamily.backend.order.infrastructure.persistence.mapper.OrderItemMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.OrderMainMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.PaymentTransactionMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.ServiceReviewMapper;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductDO;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductFamilyCardPlanDO;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductValueAddedSkuDO;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductFamilyCardPlanMapper;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductMapper;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductValueAddedSkuMapper;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.wechat.config.WechatProperties;
import com.stufamily.backend.wechat.gateway.WechatPayGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OrderApplicationServiceTest {

    @Mock
    private WechatPayGateway wechatPayGateway;
    @Mock
    private OrderMainMapper orderMainMapper;
    @Mock
    private PaymentTransactionMapper paymentTransactionMapper;
    @Mock
    private ServiceReviewMapper serviceReviewMapper;
    @Mock
    private FamilyGroupMapper familyGroupMapper;
    @Mock
    private FamilyMemberCardMapper familyMemberCardMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductFamilyCardPlanMapper familyCardPlanMapper;
    @Mock
    private ProductValueAddedSkuMapper valueAddedSkuMapper;
    @Mock
    private OrderItemMapper orderItemMapper;

    private OrderApplicationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        WechatProperties properties = new WechatProperties();
        properties.getPay().setNotifyUrl("https://notify");
        service = new OrderApplicationService(
            wechatPayGateway,
            properties,
            new ObjectMapper(),
            orderMainMapper,
            paymentTransactionMapper,
            serviceReviewMapper,
            familyGroupMapper,
            familyMemberCardMapper,
            productMapper,
            familyCardPlanMapper,
            valueAddedSkuMapper,
            orderItemMapper
        );
    }

    @Test
    void createOrderAndNotifyShouldUpdateStatus() {
        when(wechatPayGateway.createMiniappOrder(any()))
            .thenReturn(new WechatPayCreateResponse("prepay", "nonce", "sign", "123"));
        when(orderMainMapper.insert(any(OrderMainDO.class))).thenAnswer(invocation -> {
            OrderMainDO order = invocation.getArgument(0);
            order.setId(10L);
            return 1;
        });
        ProductDO product = new ProductDO();
        product.setId(11L);
        product.setProductType("FAMILY_CARD");
        product.setTitle("Family Card");
        product.setPublishStatus("ON_SHELF");
        product.setDeleted(false);
        product.setSaleStartAt(OffsetDateTime.now().minusDays(1));
        product.setSaleEndAt(OffsetDateTime.now().plusDays(365));
        when(productMapper.selectById(11L)).thenReturn(product);
        ProductFamilyCardPlanDO plan = new ProductFamilyCardPlanDO();
        plan.setId(101L);
        plan.setProductId(11L);
        plan.setDurationType("YEAR");
        plan.setDurationMonths(12);
        plan.setPriceCents(199900L);
        plan.setMaxFamilyMembers(5);
        plan.setEnabled(true);
        when(familyCardPlanMapper.selectOne(any())).thenReturn(plan);
        when(orderItemMapper.insert(any(OrderItemDO.class))).thenReturn(1);
        when(paymentTransactionMapper.insert(any(PaymentTransactionDO.class))).thenReturn(1);

        var create = service.createOrder(
            new CreateOrderCommand(1L, "FAMILY_CARD", 11L, null, "YEAR", LocalDate.parse("2026-03-30"),
                "Tom", "S1001", "13800138000", 199900L, "127.0.0.1"),
            "openid-1");
        assertEquals("PENDING_PAYMENT", create.status());

        PaymentTransactionDO payment = new PaymentTransactionDO();
        payment.setOrderId(10L);
        payment.setOutTradeNo(create.orderNo());
        payment.setProductMetaSnapshot(
            "{\"productId\":11,\"durationType\":\"YEAR\",\"applicantName\":\"Tom\","
                + "\"applicantStudentOrCardNo\":\"S1001\",\"applicantPhone\":\"13800138000\",\"cardApplyDate\":\"2026-03-30\"}");
        when(paymentTransactionMapper.selectOne(any())).thenReturn(payment);

        OrderMainDO order = new OrderMainDO();
        order.setId(10L);
        order.setBuyerUserId(1L);
        order.setOrderType("FAMILY_CARD");
        order.setPayableAmountCents(199900L);
        order.setOrderStatus("PENDING_PAYMENT");
        when(orderMainMapper.selectById(10L)).thenReturn(order);
        when(orderMainMapper.updateById(any(OrderMainDO.class))).thenReturn(1);
        when(paymentTransactionMapper.updateById(any(PaymentTransactionDO.class))).thenReturn(1);
        when(familyGroupMapper.selectOne(any())).thenReturn(null);
        when(familyGroupMapper.insert(any(FamilyGroupDO.class))).thenReturn(1);
        when(familyMemberCardMapper.selectOne(any())).thenReturn(null);
        when(familyMemberCardMapper.insert(any(FamilyMemberCardDO.class))).thenReturn(1);

        service.markPaid(new PayNotifyCommand(create.orderNo(), "wx-tx-1", 199900L));
        assertEquals("PAID", order.getOrderStatus());
        verify(familyMemberCardMapper).insert(any(FamilyMemberCardDO.class));
    }

    @Test
    void markPaidShouldFailWhenAmountMismatch() {
        PaymentTransactionDO payment = new PaymentTransactionDO();
        payment.setOrderId(10L);
        when(paymentTransactionMapper.selectOne(any())).thenReturn(payment);

        OrderMainDO order = new OrderMainDO();
        order.setId(10L);
        order.setPayableAmountCents(100L);
        when(orderMainMapper.selectById(10L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> service.markPaid(new PayNotifyCommand("ORD1", "wx", 99L)));
    }

    @Test
    void createOrderShouldFailWhenOpenidIsEmpty() {
        assertThrows(BusinessException.class,
            () -> service.createOrder(
                new CreateOrderCommand(1L, "FAMILY_CARD", 11L, null, "YEAR", LocalDate.parse("2026-03-30"),
                    "Tom", "S1001", "13800138000", 100L, "127.0.0.1"),
                ""));
    }

    @Test
    void markPaidShouldFailWhenOrderNotFound() {
        when(paymentTransactionMapper.selectOne(any())).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.markPaid(new PayNotifyCommand("NOT_EXIST", "TX", 100L)));
    }

    @Test
    void listPurchasedProductsShouldReturnPagedItems() {
        when(orderItemMapper.countPaidProductsByBuyerUserId(1L, "FAMILY_CARD")).thenReturn(1L);
        OrderPurchasedProductRowDO row = new OrderPurchasedProductRowDO();
        row.setOrderNo("ORD-PAID-1");
        row.setOrderType("FAMILY_CARD");
        row.setOrderStatus("PAID");
        row.setPaidAt(OffsetDateTime.parse("2026-03-26T10:00:00+08:00"));
        row.setProductId(101L);
        row.setProductType("FAMILY_CARD");
        row.setProductTitle("Family Card");
        row.setProductBrief("Brief");
        row.setProductImageUrls("[\"https://example.com/p1.png\"]");
        row.setSelectedDurationType("YEAR");
        row.setSelectedDurationMonths(12);
        row.setServiceStartAt(OffsetDateTime.parse("2026-03-26T10:00:00+08:00"));
        row.setServiceEndAt(OffsetDateTime.parse("2027-03-26T10:00:00+08:00"));
        row.setUnitPriceCents(19900L);
        row.setQuantity(1);
        row.setTotalPriceCents(19900L);
        row.setReviewStars(5);
        row.setReviewContent("服务很满意");
        row.setReviewedAt(OffsetDateTime.parse("2026-03-27T10:00:00+08:00"));
        when(orderItemMapper.selectPaidProductsByBuyerUserId(1L, "FAMILY_CARD", 10, 0)).thenReturn(List.of(row));

        var result = service.listPurchasedProducts(1L, "FAMILY_CARD", 1, 10);
        assertEquals(1, result.items().size());
        assertEquals("ORD-PAID-1", result.items().get(0).orderNo());
        assertEquals(5, result.items().get(0).reviewStars());
        assertEquals("服务很满意", result.items().get(0).reviewContent());
        assertEquals(1L, result.total());
    }

    @Test
    void listPurchasedProductsShouldFailWhenProductTypeInvalid() {
        assertThrows(BusinessException.class, () -> service.listPurchasedProducts(1L, "INVALID", 1, 10));
    }

    @Test
    void listPurchasedProductsShouldQueryAllWhenProductTypeMissing() {
        when(orderItemMapper.countPaidProductsByBuyerUserId(1L, null)).thenReturn(0L);

        var result = service.listPurchasedProducts(1L, null, 1, 10);

        assertEquals(0, result.items().size());
        assertEquals(0L, result.total());
    }

    @Test
    void createOrderShouldUseMockPayResponseWhenEnabled() {
        WechatProperties properties = new WechatProperties();
        properties.getPay().setNotifyUrl("https://notify");
        properties.getPay().setMockCreateOrderEnabled(true);
        OrderApplicationService mockPayService = new OrderApplicationService(
            wechatPayGateway,
            properties,
            new ObjectMapper(),
            orderMainMapper,
            paymentTransactionMapper,
            serviceReviewMapper,
            familyGroupMapper,
            familyMemberCardMapper,
            productMapper,
            familyCardPlanMapper,
            valueAddedSkuMapper,
            orderItemMapper
        );
        when(orderMainMapper.insert(any(OrderMainDO.class))).thenAnswer(invocation -> {
            OrderMainDO order = invocation.getArgument(0);
            order.setId(10L);
            return 1;
        });
        ProductDO product = new ProductDO();
        product.setId(11L);
        product.setProductType("FAMILY_CARD");
        product.setTitle("Family Card");
        product.setPublishStatus("ON_SHELF");
        product.setDeleted(false);
        product.setSaleStartAt(OffsetDateTime.now().minusDays(1));
        product.setSaleEndAt(OffsetDateTime.now().plusDays(365));
        when(productMapper.selectById(11L)).thenReturn(product);
        ProductFamilyCardPlanDO plan = new ProductFamilyCardPlanDO();
        plan.setId(101L);
        plan.setProductId(11L);
        plan.setDurationType("YEAR");
        plan.setDurationMonths(12);
        plan.setPriceCents(199900L);
        plan.setMaxFamilyMembers(5);
        plan.setEnabled(true);
        when(familyCardPlanMapper.selectOne(any())).thenReturn(plan);
        when(orderItemMapper.insert(any(OrderItemDO.class))).thenReturn(1);
        when(paymentTransactionMapper.insert(any(PaymentTransactionDO.class))).thenReturn(1);

        var create = mockPayService.createOrder(
            new CreateOrderCommand(1L, "FAMILY_CARD", 11L, null, "YEAR", LocalDate.parse("2026-03-30"),
                "Tom", "S1001", "13800138000", 199900L, "127.0.0.1"),
            "openid-1"
        );

        verify(wechatPayGateway, never()).createMiniappOrder(any());
        assertEquals("PENDING_PAYMENT", create.status());
        assertEquals("mock_prepay_" + create.orderNo(), create.payParams().prepayId());
    }

    @Test
    void createOrderShouldUseSpecifiedValueAddedSkuWhenSkuIdProvided() {
        when(wechatPayGateway.createMiniappOrder(any()))
            .thenReturn(new WechatPayCreateResponse("prepay", "nonce", "sign", "123"));
        when(orderMainMapper.insert(any(OrderMainDO.class))).thenAnswer(invocation -> {
            OrderMainDO order = invocation.getArgument(0);
            order.setId(20L);
            return 1;
        });
        when(orderItemMapper.insert(any(OrderItemDO.class))).thenReturn(1);
        when(paymentTransactionMapper.insert(any(PaymentTransactionDO.class))).thenReturn(1);

        ProductDO product = new ProductDO();
        product.setId(12L);
        product.setProductType("VALUE_ADDED_SERVICE");
        product.setTitle("增值服务");
        product.setPublishStatus("ON_SHELF");
        product.setDeleted(false);
        product.setSaleStartAt(OffsetDateTime.now().minusDays(1));
        product.setSaleEndAt(OffsetDateTime.now().plusDays(30));
        when(productMapper.selectById(12L)).thenReturn(product);

        ProductValueAddedSkuDO sku = new ProductValueAddedSkuDO();
        sku.setId(202L);
        sku.setProductId(12L);
        sku.setPriceCents(29900L);
        sku.setEnabled(true);
        when(valueAddedSkuMapper.selectOne(any())).thenReturn(sku);

        var create = service.createOrder(
            new CreateOrderCommand(1L, "VALUE_ADDED_SERVICE", 12L, 202L, null, null, null, null, null, 1L,
                "127.0.0.1"),
            "openid-1"
        );

        assertEquals(29900L, create.payableAmountCents());
    }

    @Test
    void createOrderShouldFailWhenCardApplyDateMissingForFamilyCard() {
        ProductDO product = new ProductDO();
        product.setId(11L);
        product.setProductType("FAMILY_CARD");
        product.setTitle("Family Card");
        product.setPublishStatus("ON_SHELF");
        product.setDeleted(false);
        product.setSaleStartAt(OffsetDateTime.now().minusDays(1));
        product.setSaleEndAt(OffsetDateTime.now().plusDays(365));
        when(productMapper.selectById(11L)).thenReturn(product);

        assertThrows(BusinessException.class,
            () -> service.createOrder(
                new CreateOrderCommand(1L, "FAMILY_CARD", 11L, null, "YEAR", null, "Tom", "S1001",
                    "13800138000", 199900L, "127.0.0.1"),
                "openid-1"));
    }

    @Test
    void createOrderShouldFailWhenApplicantNameMissingForFamilyCard() {
        ProductDO product = new ProductDO();
        product.setId(11L);
        product.setProductType("FAMILY_CARD");
        product.setTitle("Family Card");
        product.setPublishStatus("ON_SHELF");
        product.setDeleted(false);
        product.setSaleStartAt(OffsetDateTime.now().minusDays(1));
        product.setSaleEndAt(OffsetDateTime.now().plusDays(365));
        when(productMapper.selectById(11L)).thenReturn(product);

        assertThrows(BusinessException.class,
            () -> service.createOrder(
                new CreateOrderCommand(1L, "FAMILY_CARD", 11L, null, "YEAR", LocalDate.parse("2026-03-30"),
                    "", "S1001", "13800138000", 199900L, "127.0.0.1"),
                "openid-1"));
    }

    @Test
    void markPaidShouldNotInsertDuplicateFamilyMemberOnRepeatedNotify() {
        PaymentTransactionDO payment = new PaymentTransactionDO();
        payment.setOrderId(10L);
        payment.setOutTradeNo("ORD_REPEAT_1");
        payment.setProductMetaSnapshot(
            "{\"productId\":11,\"durationType\":\"YEAR\",\"applicantName\":\"Tom\","
                + "\"applicantStudentOrCardNo\":\"S1001\",\"applicantPhone\":\"13800138000\","
                + "\"cardApplyDate\":\"2026-03-30\",\"maxMembers\":5}");
        when(paymentTransactionMapper.selectOne(any())).thenReturn(payment, payment);

        OrderMainDO order = new OrderMainDO();
        order.setId(10L);
        order.setOrderNo("ORD_REPEAT_1");
        order.setBuyerUserId(1L);
        order.setOrderType("FAMILY_CARD");
        order.setPayableAmountCents(199900L);
        order.setOrderStatus("PENDING_PAYMENT");
        when(orderMainMapper.selectById(10L)).thenReturn(order, order);
        when(orderMainMapper.updateById(any(OrderMainDO.class))).thenReturn(1);
        when(paymentTransactionMapper.updateById(any(PaymentTransactionDO.class))).thenReturn(1);

        FamilyGroupDO existingGroup = new FamilyGroupDO();
        existingGroup.setId(20L);
        existingGroup.setGroupNo("FG_EXIST_1");
        existingGroup.setOwnerUserId(1L);
        existingGroup.setCurrentMembers(1);
        existingGroup.setMaxMembers(5);
        when(familyGroupMapper.selectOne(any())).thenReturn(null, existingGroup);
        when(familyGroupMapper.insert(any(FamilyGroupDO.class))).thenAnswer(invocation -> {
            FamilyGroupDO group = invocation.getArgument(0);
            group.setId(20L);
            return 1;
        });
        when(familyGroupMapper.updateById(any(FamilyGroupDO.class))).thenReturn(1);

        FamilyMemberCardDO existingMember = new FamilyMemberCardDO();
        existingMember.setId(30L);
        existingMember.setGroupId(20L);
        existingMember.setStudentOrCardNo("S1001");
        when(familyMemberCardMapper.selectOne(any())).thenReturn(null, existingMember);
        when(familyMemberCardMapper.insert(any(FamilyMemberCardDO.class))).thenReturn(1);

        service.markPaid(new PayNotifyCommand("ORD_REPEAT_1", "TX_1", 199900L));
        service.markPaid(new PayNotifyCommand("ORD_REPEAT_1", "TX_1_RETRY", 199900L));

        verify(familyMemberCardMapper, times(1)).insert(any(FamilyMemberCardDO.class));
        verify(familyGroupMapper, times(1)).insert(any(FamilyGroupDO.class));
    }

    @Test
    void submitServiceReviewShouldInsertWhenNotExists() {
        OrderMainDO order = new OrderMainDO();
        order.setId(88L);
        order.setOrderNo("ORD-PAID-REVIEW-1");
        order.setBuyerUserId(1L);
        order.setOrderStatus("PAID");
        when(orderMainMapper.selectOne(any())).thenReturn(order);
        when(serviceReviewMapper.selectOne(any())).thenReturn(null);

        OrderItemDO item = new OrderItemDO();
        item.setProductId(101L);
        item.setProductTypeSnapshot("FAMILY_CARD");
        when(orderItemMapper.selectOne(any())).thenReturn(item);

        service.submitServiceReview(new SubmitServiceReviewCommand(1L, "ORD-PAID-REVIEW-1", 5, "很好"));

        verify(serviceReviewMapper, times(1)).insert(any(ServiceReviewDO.class));
    }

    @Test
    void submitServiceReviewShouldUpdateWhenExists() {
        OrderMainDO order = new OrderMainDO();
        order.setId(89L);
        order.setOrderNo("ORD-PAID-REVIEW-2");
        order.setBuyerUserId(1L);
        order.setOrderStatus("PAID");
        when(orderMainMapper.selectOne(any())).thenReturn(order);

        ServiceReviewDO existing = new ServiceReviewDO();
        existing.setId(999L);
        existing.setOrderId(89L);
        existing.setStars(3);
        existing.setContent("old");
        when(serviceReviewMapper.selectOne(any())).thenReturn(existing);

        service.submitServiceReview(new SubmitServiceReviewCommand(1L, "ORD-PAID-REVIEW-2", 4, "更新评价"));

        verify(serviceReviewMapper, times(1)).updateById(any(ServiceReviewDO.class));
        verify(serviceReviewMapper, never()).insert(any(ServiceReviewDO.class));
    }

    @Test
    void submitServiceReviewShouldFailWhenOrderNotPaid() {
        OrderMainDO order = new OrderMainDO();
        order.setId(90L);
        order.setOrderNo("ORD-NOT-PAID");
        order.setBuyerUserId(1L);
        order.setOrderStatus("PENDING_PAYMENT");
        when(orderMainMapper.selectOne(any())).thenReturn(order);

        assertThrows(BusinessException.class,
            () -> service.submitServiceReview(new SubmitServiceReviewCommand(1L, "ORD-NOT-PAID", 5, "很好")));
    }
}
