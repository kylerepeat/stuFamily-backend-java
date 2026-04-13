package com.stufamily.backend.order.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

@TableName("order_item")
public class OrderItemDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("order_id")
    private Long orderId;
    @TableField("product_id")
    private Long productId;
    @TableField("product_type_snapshot")
    private String productTypeSnapshot;
    @TableField("product_title_snapshot")
    private String productTitleSnapshot;
    @TableField("product_brief_snapshot")
    private String productBriefSnapshot;
    @TableField("product_detail_snapshot")
    private String productDetailSnapshot;
    @TableField("selected_duration_type")
    private String selectedDurationType;
    @TableField("selected_duration_months")
    private Integer selectedDurationMonths;
    @TableField("service_start_at")
    private OffsetDateTime serviceStartAt;
    @TableField("service_end_at")
    private OffsetDateTime serviceEndAt;
    @TableField("unit_price_cents")
    private Long unitPriceCents;
    @TableField("quantity")
    private Integer quantity;
    @TableField("total_price_cents")
    private Long totalPriceCents;
    @TableField("created_at")
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductTypeSnapshot() {
        return productTypeSnapshot;
    }

    public void setProductTypeSnapshot(String productTypeSnapshot) {
        this.productTypeSnapshot = productTypeSnapshot;
    }

    public String getProductTitleSnapshot() {
        return productTitleSnapshot;
    }

    public void setProductTitleSnapshot(String productTitleSnapshot) {
        this.productTitleSnapshot = productTitleSnapshot;
    }

    public String getProductBriefSnapshot() {
        return productBriefSnapshot;
    }

    public void setProductBriefSnapshot(String productBriefSnapshot) {
        this.productBriefSnapshot = productBriefSnapshot;
    }

    public String getProductDetailSnapshot() {
        return productDetailSnapshot;
    }

    public void setProductDetailSnapshot(String productDetailSnapshot) {
        this.productDetailSnapshot = productDetailSnapshot;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

