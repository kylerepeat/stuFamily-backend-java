package com.stufamily.backend.order.infrastructure.persistence.dataobject;

import java.time.OffsetDateTime;

public class OrderPurchasedProductRowDO {
    private String orderNo;
    private String orderType;
    private String orderStatus;
    private OffsetDateTime paidAt;
    private Long productId;
    private String productType;
    private String productTitle;
    private String productBrief;
    private String productImageUrls;
    private String selectedDurationType;
    private Integer selectedDurationMonths;
    private OffsetDateTime serviceStartAt;
    private OffsetDateTime serviceEndAt;
    private Long unitPriceCents;
    private Integer quantity;
    private Long totalPriceCents;
    private Integer reviewStars;
    private String reviewContent;
    private OffsetDateTime reviewedAt;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public String getProductBrief() {
        return productBrief;
    }

    public void setProductBrief(String productBrief) {
        this.productBrief = productBrief;
    }

    public String getProductImageUrls() {
        return productImageUrls;
    }

    public void setProductImageUrls(String productImageUrls) {
        this.productImageUrls = productImageUrls;
    }

    public String getSelectedDurationType() {
        return selectedDurationType;
    }

    public void setSelectedDurationType(String selectedDurationType) {
        this.selectedDurationType = selectedDurationType;
    }

    public Integer getSelectedDurationMonths() {
        return selectedDurationMonths;
    }

    public void setSelectedDurationMonths(Integer selectedDurationMonths) {
        this.selectedDurationMonths = selectedDurationMonths;
    }

    public OffsetDateTime getServiceStartAt() {
        return serviceStartAt;
    }

    public void setServiceStartAt(OffsetDateTime serviceStartAt) {
        this.serviceStartAt = serviceStartAt;
    }

    public OffsetDateTime getServiceEndAt() {
        return serviceEndAt;
    }

    public void setServiceEndAt(OffsetDateTime serviceEndAt) {
        this.serviceEndAt = serviceEndAt;
    }

    public Long getUnitPriceCents() {
        return unitPriceCents;
    }

    public void setUnitPriceCents(Long unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getTotalPriceCents() {
        return totalPriceCents;
    }

    public void setTotalPriceCents(Long totalPriceCents) {
        this.totalPriceCents = totalPriceCents;
    }

    public Integer getReviewStars() {
        return reviewStars;
    }

    public void setReviewStars(Integer reviewStars) {
        this.reviewStars = reviewStars;
    }

    public String getReviewContent() {
        return reviewContent;
    }

    public void setReviewContent(String reviewContent) {
        this.reviewContent = reviewContent;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
