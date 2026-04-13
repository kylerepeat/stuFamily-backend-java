package com.stufamily.backend.order.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

@TableName("order_main")
public class OrderMainDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("order_no")
    private String orderNo;
    @TableField("buyer_user_id")
    private Long buyerUserId;
    @TableField("order_type")
    private String orderType;
    @TableField("order_status")
    private String orderStatus;
    @TableField("total_amount_cents")
    private Long totalAmountCents;
    @TableField("discount_amount_cents")
    private Long discountAmountCents;
    @TableField("payable_amount_cents")
    private Long payableAmountCents;
    @TableField("currency")
    private String currency;
    @TableField("source_channel")
    private String sourceChannel;
    @TableField("client_ip")
    private String clientIp;
    @TableField("expire_at")
    private OffsetDateTime expireAt;
    @TableField("paid_at")
    private OffsetDateTime paidAt;
    @TableField("created_at")
    private OffsetDateTime createdAt;
    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getBuyerUserId() {
        return buyerUserId;
    }

    public void setBuyerUserId(Long buyerUserId) {
        this.buyerUserId = buyerUserId;
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

    public Long getTotalAmountCents() {
        return totalAmountCents;
    }

    public void setTotalAmountCents(Long totalAmountCents) {
        this.totalAmountCents = totalAmountCents;
    }

    public Long getDiscountAmountCents() {
        return discountAmountCents;
    }

    public void setDiscountAmountCents(Long discountAmountCents) {
        this.discountAmountCents = discountAmountCents;
    }

    public Long getPayableAmountCents() {
        return payableAmountCents;
    }

    public void setPayableAmountCents(Long payableAmountCents) {
        this.payableAmountCents = payableAmountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public OffsetDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(OffsetDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

