package com.stufamily.backend.order.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

@TableName("payment_transaction")
public class PaymentTransactionDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("payment_no")
    private String paymentNo;
    @TableField("order_id")
    private Long orderId;
    @TableField("payment_status")
    private String paymentStatus;
    @TableField("channel")
    private String channel;
    @TableField("out_trade_no")
    private String outTradeNo;
    @TableField("transaction_id")
    private String transactionId;
    @TableField("payer_openid")
    private String payerOpenid;
    @TableField("total_amount_cents")
    private Long totalAmountCents;
    @TableField("currency")
    private String currency;
    @TableField("product_type_snapshot")
    private String productTypeSnapshot;
    @TableField("product_title_snapshot")
    private String productTitleSnapshot;
    @TableField("product_meta_snapshot")
    private String productMetaSnapshot;
    @TableField("success_time")
    private OffsetDateTime successTime;
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

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPayerOpenid() {
        return payerOpenid;
    }

    public void setPayerOpenid(String payerOpenid) {
        this.payerOpenid = payerOpenid;
    }

    public Long getTotalAmountCents() {
        return totalAmountCents;
    }

    public void setTotalAmountCents(Long totalAmountCents) {
        this.totalAmountCents = totalAmountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public String getProductMetaSnapshot() {
        return productMetaSnapshot;
    }

    public void setProductMetaSnapshot(String productMetaSnapshot) {
        this.productMetaSnapshot = productMetaSnapshot;
    }

    public OffsetDateTime getSuccessTime() {
        return successTime;
    }

    public void setSuccessTime(OffsetDateTime successTime) {
        this.successTime = successTime;
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

