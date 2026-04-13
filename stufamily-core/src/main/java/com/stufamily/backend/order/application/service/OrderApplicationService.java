package com.stufamily.backend.order.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyGroupDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyMemberCardDO;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyGroupMapper;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyMemberCardMapper;
import com.stufamily.backend.order.application.command.CreateOrderCommand;
import com.stufamily.backend.order.application.command.PayNotifyCommand;
import com.stufamily.backend.order.application.command.SubmitServiceReviewCommand;
import com.stufamily.backend.order.application.dto.OrderCreateResult;
import com.stufamily.backend.order.application.dto.PurchasedProductView;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderMainDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderItemDO;
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
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import com.stufamily.backend.wechat.config.WechatProperties;
import com.stufamily.backend.wechat.gateway.WechatPayGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateRequest;
import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
public class OrderApplicationService {
    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;


    private final WechatPayGateway wechatPayGateway;
    private final WechatProperties properties;
    private final ObjectMapper objectMapper;
    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final ServiceReviewMapper serviceReviewMapper;
    private final FamilyGroupMapper familyGroupMapper;
    private final FamilyMemberCardMapper familyMemberCardMapper;
    private final ProductMapper productMapper;
    private final ProductFamilyCardPlanMapper familyCardPlanMapper;
    private final ProductValueAddedSkuMapper valueAddedSkuMapper;

    public OrderApplicationService(WechatPayGateway wechatPayGateway, WechatProperties properties,
                                   ObjectMapper objectMapper, OrderMainMapper orderMainMapper,
                                   PaymentTransactionMapper paymentTransactionMapper, ServiceReviewMapper serviceReviewMapper,
                                   FamilyGroupMapper familyGroupMapper,
                                   FamilyMemberCardMapper familyMemberCardMapper, ProductMapper productMapper,
                                   ProductFamilyCardPlanMapper familyCardPlanMapper,
                                   ProductValueAddedSkuMapper valueAddedSkuMapper, OrderItemMapper orderItemMapper) {
        this.wechatPayGateway = wechatPayGateway;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.orderMainMapper = orderMainMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentTransactionMapper = paymentTransactionMapper;
        this.serviceReviewMapper = serviceReviewMapper;
        this.familyGroupMapper = familyGroupMapper;
        this.familyMemberCardMapper = familyMemberCardMapper;
        this.productMapper = productMapper;
        this.familyCardPlanMapper = familyCardPlanMapper;
        this.valueAddedSkuMapper = valueAddedSkuMapper;
    }

    @Transactional
    public OrderCreateResult createOrder(CreateOrderCommand command, String openid) {
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "openid is required for wechat pay");
        }
        ProductDO product = requireProduct(command.productId(), command.productType());
        PricingResult pricing = resolvePricing(product, command);
        OffsetDateTime now = OffsetDateTime.now();
        String orderNo = "ORD" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);
        OrderMainDO order = new OrderMainDO();
        order.setOrderNo(orderNo);
        order.setBuyerUserId(command.userId());
        order.setOrderType(toOrderType(product.getProductType()));
        order.setOrderStatus("PENDING_PAYMENT");
        order.setTotalAmountCents(pricing.priceCents());
        order.setDiscountAmountCents(0L);
        order.setPayableAmountCents(pricing.priceCents());
        order.setCurrency("CNY");
        order.setSourceChannel("WEIXIN_MINIAPP");
        order.setClientIp(command.clientIp());
        order.setExpireAt(now.plusMinutes(30));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMainMapper.insert(order);
        persistOrderItem(order, product, pricing, command, now);

        PaymentTransactionDO payment = new PaymentTransactionDO();
        payment.setPaymentNo("PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT));
        payment.setOrderId(order.getId());
        payment.setPaymentStatus("INITIATED");
        payment.setChannel("WECHAT_PAY");
        payment.setOutTradeNo(orderNo);
        payment.setPayerOpenid(openid);
        payment.setTotalAmountCents(pricing.priceCents());
        payment.setCurrency("CNY");
        payment.setProductTypeSnapshot(toProductType(product.getProductType()));
        payment.setProductTitleSnapshot(product.getTitle());
        payment.setProductMetaSnapshot(toProductMeta(command, pricing));
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        paymentTransactionMapper.insert(payment);

        WechatPayCreateRequest payRequest = new WechatPayCreateRequest(
            orderNo,
            openid,
            product.getTitle(),
            pricing.priceCents(),
            properties.getPay().getNotifyUrl(),
            command.clientIp()
        );
        WechatPayCreateResponse payResponse = createWechatPayResponse(payRequest);
        return new OrderCreateResult(orderNo, order.getOrderStatus(), order.getPayableAmountCents(), payResponse);
    }

    private WechatPayCreateResponse createWechatPayResponse(WechatPayCreateRequest payRequest) {
        if (properties.getPay().isMockCreateOrderEnabled()) {
            log.info("createOrder uses mock wechat pay, outTradeNo={}", payRequest.outTradeNo());
            return new WechatPayCreateResponse(
                "mock_prepay_" + payRequest.outTradeNo(),
                UUID.randomUUID().toString().replace("-", ""),
                "mock_sign_" + payRequest.outTradeNo(),
                String.valueOf(Instant.now().getEpochSecond())
            );
        }
        return wechatPayGateway.createMiniappOrder(payRequest);
    }

    @Transactional
    public void markPaid(PayNotifyCommand command) {
        PaymentTransactionDO payment = paymentTransactionMapper.selectOne(
            new LambdaQueryWrapper<PaymentTransactionDO>().eq(PaymentTransactionDO::getOutTradeNo, command.outTradeNo())
        );
        if (payment == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "order not found");
        }
        OrderMainDO order = orderMainMapper.selectById(payment.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "order not found");
        }
        if (command.totalAmountCents() != order.getPayableAmountCents()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "payment amount mismatch");
        }
        OffsetDateTime now = OffsetDateTime.now();
        order.setOrderStatus("PAID");
        order.setPaidAt(now);
        order.setUpdatedAt(now);
        orderMainMapper.updateById(order);

        payment.setPaymentStatus("SUCCESS");
        payment.setTransactionId(command.transactionId());
        payment.setSuccessTime(now);
        payment.setUpdatedAt(now);
        paymentTransactionMapper.updateById(payment);

        if ("FAMILY_CARD".equals(order.getOrderType())) {
            createFamilyGroupIfAbsent(order, payment, now);
        }
    }

    @Transactional(readOnly = true)
    public String findOrderStatus(String orderNo) {
        OrderMainDO order = orderMainMapper.selectOne(
            new LambdaQueryWrapper<OrderMainDO>().eq(OrderMainDO::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "order not found");
        }
        return order.getOrderStatus();
    }

    @Transactional(readOnly = true)
    public PageResult<PurchasedProductView> listPurchasedProducts(
        Long buyerUserId, String productType, Integer pageNo, Integer pageSize) {
        String normalizedProductType = normalizePurchasedProductType(productType);
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        long total = orderItemMapper.countPaidProductsByBuyerUserId(buyerUserId, normalizedProductType);
        if (total <= 0) {
            return PageResult.of(List.of(), 0, normalizedPageNo, normalizedPageSize);
        }
        List<PurchasedProductView> items = orderItemMapper
            .selectPaidProductsByBuyerUserId(buyerUserId, normalizedProductType, normalizedPageSize, offset)
            .stream()
            .map(this::toPurchasedProductView)
            .toList();
        return PageResult.of(items, total, normalizedPageNo, normalizedPageSize);
    }

    @Transactional
    public void submitServiceReview(SubmitServiceReviewCommand command) {
        if (command.stars() == null || command.stars() < 1 || command.stars() > 5) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "stars must be between 1 and 5");
        }
        String content = normalize(command.content());
        if (content.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "content length must be less than or equal to 500");
        }
        OrderMainDO order = orderMainMapper.selectOne(
            new LambdaQueryWrapper<OrderMainDO>()
                .eq(OrderMainDO::getOrderNo, command.orderNo())
                .eq(OrderMainDO::getBuyerUserId, command.userId())
                .last("limit 1")
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "order not found");
        }
        if (!"PAID".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "only paid order can be reviewed");
        }

        OffsetDateTime now = OffsetDateTime.now();
        ServiceReviewDO existing = serviceReviewMapper.selectOne(
            new LambdaQueryWrapper<ServiceReviewDO>()
                .eq(ServiceReviewDO::getOrderId, order.getId())
                .last("limit 1")
        );
        if (existing != null) {
            existing.setStars(command.stars());
            existing.setContent(content);
            existing.setUpdatedAt(now);
            serviceReviewMapper.updateById(existing);
            return;
        }

        ServiceReviewDO review = new ServiceReviewDO();
        review.setOrderId(order.getId());
        review.setOrderNo(order.getOrderNo());
        review.setBuyerUserId(order.getBuyerUserId());
        review.setStars(command.stars());
        review.setContent(content);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        OrderItemDO orderItem = orderItemMapper.selectOne(
            new LambdaQueryWrapper<OrderItemDO>()
                .eq(OrderItemDO::getOrderId, order.getId())
                .orderByDesc(OrderItemDO::getId)
                .last("limit 1")
        );
        if (orderItem != null) {
            review.setProductId(orderItem.getProductId());
            review.setProductType(orderItem.getProductTypeSnapshot());
        }
        serviceReviewMapper.insert(review);
    }

    private void createFamilyGroupIfAbsent(OrderMainDO order, PaymentTransactionDO payment, OffsetDateTime now) {
        FamilyGroupDO group = familyGroupMapper.selectOne(
            new LambdaQueryWrapper<FamilyGroupDO>().eq(FamilyGroupDO::getSourceOrderId, order.getId())
        );
        if (group == null) {
            Map<String, Object> meta = parseMeta(payment.getProductMetaSnapshot());
            Long productId = meta.get("productId") instanceof Number n ? n.longValue() : 1L;
            Long planId = meta.get("planId") instanceof Number n ? n.longValue() : null;
            String durationType = String.valueOf(meta.getOrDefault("durationType", "MONTH"));
            int maxMembers = meta.get("maxMembers") instanceof Number n ? n.intValue() : 1;

            group = new FamilyGroupDO();
            group.setGroupNo("FG" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
            group.setSourceOrderId(order.getId());
            group.setOwnerUserId(order.getBuyerUserId());
            group.setFamilyCardProductId(productId);
            group.setFamilyCardPlanId(planId);
            group.setMaxMembers(maxMembers);
            group.setCurrentMembers(0);
            group.setStatus("ACTIVE");
            group.setActivatedAt(now);
            group.setExpireAt(now.plusMonths(durationToMonths(durationType)));
            group.setCreatedAt(now);
            group.setUpdatedAt(now);
            familyGroupMapper.insert(group);
        }
        ensureApplicantMemberFromOrder(order, payment, group, now);
    }

    private void ensureApplicantMemberFromOrder(OrderMainDO order, PaymentTransactionDO payment,
                                                FamilyGroupDO group, OffsetDateTime now) {
        Map<String, Object> meta = parseMeta(payment.getProductMetaSnapshot());
        String applicantName = normalize(asString(meta.get("applicantName")));
        String applicantStudentOrCardNo = normalize(asString(meta.get("applicantStudentOrCardNo")));
        String applicantPhone = normalize(asString(meta.get("applicantPhone")));
        String cardApplyDateText = normalize(asString(meta.get("cardApplyDate")));
        if (!StringUtils.hasText(applicantName)
            || !StringUtils.hasText(applicantStudentOrCardNo)
            || !StringUtils.hasText(applicantPhone)) {
            log.warn("skip auto create family member from order, missing applicant info, orderNo={}", order.getOrderNo());
            return;
        }
        FamilyMemberCardDO existingMember = familyMemberCardMapper.selectOne(
            new LambdaQueryWrapper<FamilyMemberCardDO>()
                .eq(FamilyMemberCardDO::getGroupId, group.getId())
                .eq(FamilyMemberCardDO::getStudentOrCardNo, applicantStudentOrCardNo)
                .last("limit 1")
        );
        if (existingMember != null) {
            return;
        }
        int currentMembers = group.getCurrentMembers() == null ? 0 : group.getCurrentMembers();
        int maxMembers = group.getMaxMembers() == null ? 0 : group.getMaxMembers();
        if (maxMembers > 0 && currentMembers >= maxMembers) {
            log.warn("skip auto create family member from order, group is full, groupNo={}, orderNo={}",
                group.getGroupNo(), order.getOrderNo());
            return;
        }
        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setGroupId(group.getId());
        member.setMemberNo("M" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT));
        member.setMemberName(applicantName);
        member.setStudentOrCardNo(applicantStudentOrCardNo);
        member.setPhone(applicantPhone);
        member.setCardReceivedDate(parseCardApplyDate(cardApplyDateText));
        member.setAddedByUserId(order.getBuyerUserId());
        member.setStatus("ACTIVE");
        member.setJoinedAt(now);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        familyMemberCardMapper.insert(member);
        group.setCurrentMembers(currentMembers + 1);
        group.setUpdatedAt(now);
        familyGroupMapper.updateById(group);
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private LocalDate parseCardApplyDate(String dateText) {
        if (!StringUtils.hasText(dateText)) {
            return null;
        }
        try {
            return LocalDate.parse(dateText);
        } catch (Exception ex) {
            return null;
        }
    }

    private long durationToMonths(String durationType) {
        return switch (durationType) {
            case "YEAR" -> 12;
            case "SEMESTER" -> 6;
            default -> 1;
        };
    }

    private String toOrderType(String productType) {
        return "FAMILY_CARD".equalsIgnoreCase(productType) ? "FAMILY_CARD" : "VALUE_ADDED_SERVICE";
    }

    private String toProductType(String productType) {
        return "FAMILY_CARD".equalsIgnoreCase(productType) ? "FAMILY_CARD" : "VALUE_ADDED_SERVICE";
    }

    private String toProductMeta(CreateOrderCommand command, PricingResult pricing) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "productId", command.productId(),
                "skuId", pricing.skuId() == null ? 0L : pricing.skuId(),
                "durationType", pricing.durationType(),
                "cardApplyDate", command.cardApplyDate() == null ? "" : command.cardApplyDate().toString(),
                "applicantName", normalize(command.applicantName()),
                "applicantStudentOrCardNo", normalize(command.applicantStudentOrCardNo()),
                "applicantPhone", normalize(command.applicantPhone()),
                "planId", pricing.planId() == null ? 0L : pricing.planId(),
                "maxMembers", pricing.maxMembers()
            ));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "serialize order product meta failed");
        }
    }

    private Map<String, Object> parseMeta(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private ProductDO requireProduct(Long productId, String productType) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || Boolean.TRUE.equals(product.getDeleted())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "product not found");
        }
        if (!"ON_SHELF".equals(product.getPublishStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "product is not on shelf");
        }
        if (!product.getProductType().equalsIgnoreCase(productType)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "product type mismatch");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (product.getSaleStartAt() != null && product.getSaleStartAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "product sale not started");
        }
        if (product.getSaleEndAt() != null && product.getSaleEndAt().isBefore(now)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "product sale has ended");
        }
        return product;
    }

    private PricingResult resolvePricing(ProductDO product, CreateOrderCommand command) {
        if ("FAMILY_CARD".equals(product.getProductType())) {
            if (!StringUtils.hasText(command.durationType())) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "durationType is required for family card");
            }
            if (command.cardApplyDate() == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "cardApplyDate is required for family card");
            }
            if (!StringUtils.hasText(command.applicantName())) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "applicantName is required for family card");
            }
            if (!StringUtils.hasText(command.applicantStudentOrCardNo())) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "applicantStudentOrCardNo is required for family card");
            }
            String applicantPhone = normalize(command.applicantPhone());
            if (!StringUtils.hasText(applicantPhone)) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "applicantPhone is required for family card");
            }
            if (!applicantPhone.matches("^1\\d{10}$")) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "applicantPhone format invalid");
            }
            ProductFamilyCardPlanDO plan = familyCardPlanMapper.selectOne(
                new LambdaQueryWrapper<ProductFamilyCardPlanDO>()
                    .eq(ProductFamilyCardPlanDO::getProductId, product.getId())
                    .eq(ProductFamilyCardPlanDO::getDurationType, command.durationType())
                    .eq(ProductFamilyCardPlanDO::getEnabled, true)
                    .last("limit 1")
            );
            if (plan == null) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "family card plan not found");
            }
            return new PricingResult(plan.getPriceCents(), plan.getDurationType(), plan.getMaxFamilyMembers(), plan.getId(),
                plan.getDurationMonths(), null);
        }
        ProductValueAddedSkuDO sku;
        if (command.skuId() != null) {
            sku = valueAddedSkuMapper.selectOne(
                new LambdaQueryWrapper<ProductValueAddedSkuDO>()
                    .eq(ProductValueAddedSkuDO::getId, command.skuId())
                    .eq(ProductValueAddedSkuDO::getProductId, product.getId())
                    .eq(ProductValueAddedSkuDO::getEnabled, true)
                    .last("limit 1")
            );
            if (sku == null) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "value added sku not found");
            }
        } else {
            sku = valueAddedSkuMapper.selectOne(
                new LambdaQueryWrapper<ProductValueAddedSkuDO>()
                    .eq(ProductValueAddedSkuDO::getProductId, product.getId())
                    .eq(ProductValueAddedSkuDO::getEnabled, true)
                    .orderByAsc(ProductValueAddedSkuDO::getId)
                    .last("limit 1")
            );
        }
        if (sku != null) {
            return new PricingResult(sku.getPriceCents(), "NONE", 0, null, null, sku.getId());
        }
        if (command.amountCents() == null || command.amountCents() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "amount must be greater than zero");
        }
        return new PricingResult(command.amountCents(), "NONE", 0, null, null, null);
    }

    private void persistOrderItem(OrderMainDO order, ProductDO product, PricingResult pricing,
                                  CreateOrderCommand command, OffsetDateTime now) {
        OrderItemDO item = new OrderItemDO();
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setProductTypeSnapshot(product.getProductType());
        item.setProductTitleSnapshot(product.getTitle());
        item.setProductBriefSnapshot(product.getDetailContent());
        item.setProductDetailSnapshot(buildOrderItemDetailSnapshot(command, pricing));
        item.setSelectedDurationType("NONE".equals(pricing.durationType()) ? null : pricing.durationType());
        item.setSelectedDurationMonths(pricing.durationMonths());
        item.setServiceStartAt(product.getServiceStartAt());
        item.setServiceEndAt(product.getServiceEndAt());
        item.setUnitPriceCents(pricing.priceCents());
        item.setQuantity(1);
        item.setTotalPriceCents(pricing.priceCents());
        item.setCreatedAt(now);
        orderItemMapper.insert(item);
    }

    private String buildOrderItemDetailSnapshot(CreateOrderCommand command, PricingResult pricing) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "skuId", pricing.skuId() == null ? 0L : pricing.skuId(),
                "durationType", pricing.durationType(),
                "cardApplyDate", command.cardApplyDate() == null ? "" : command.cardApplyDate().toString(),
                "applicantName", normalize(command.applicantName()),
                "applicantStudentOrCardNo", normalize(command.applicantStudentOrCardNo()),
                "applicantPhone", normalize(command.applicantPhone())
            ));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "serialize order item detail snapshot failed");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private PurchasedProductView toPurchasedProductView(OrderPurchasedProductRowDO row) {
        return new PurchasedProductView(
            row.getOrderNo(),
            row.getOrderType(),
            row.getOrderStatus(),
            row.getPaidAt(),
            row.getProductId(),
            row.getProductType(),
            row.getProductTitle(),
            row.getProductBrief(),
            row.getProductImageUrls(),
            row.getSelectedDurationType(),
            row.getSelectedDurationMonths(),
            row.getServiceStartAt(),
            row.getServiceEndAt(),
            row.getUnitPriceCents(),
            row.getQuantity(),
            row.getTotalPriceCents(),
            row.getReviewStars(),
            row.getReviewContent(),
            row.getReviewedAt()
        );
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

    private String normalizePurchasedProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            return null;
        }
        String normalized = productType.trim().toUpperCase(Locale.ROOT);
        if (!"FAMILY_CARD".equals(normalized) && !"VALUE_ADDED_SERVICE".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "invalid product_type");
        }
        return normalized;
    }

    private record PricingResult(Long priceCents, String durationType, Integer maxMembers, Long planId,
                                 Integer durationMonths, Long skuId) {
    }
}
