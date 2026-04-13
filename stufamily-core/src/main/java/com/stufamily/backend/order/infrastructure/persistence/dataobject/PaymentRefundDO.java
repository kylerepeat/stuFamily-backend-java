package com.stufamily.backend.order.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

@TableName("payment_refund")
public class PaymentRefundDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("payment_id")
    private Long paymentId;
    @TableField("refund_no")
    private String refundNo;
    @TableField("wechat_refund_id")
    private String wechatRefundId;
    @TableField("refund_status")
    private String refundStatus;
    @TableField("refund_amount_cents")
    private Long refundAmountCents;
    @TableField("reason")
    private String reason;
    @TableField("refund_payload")
    private String refundPayload;
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

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getRefundNo() {
        return refundNo;
    }

    public void setRefundNo(String refundNo) {
        this.refundNo = refundNo;
    }

    public String getWechatRefundId() {
        return wechatRefundId;
    }

    public void setWechatRefundId(String wechatRefundId) {
        this.wechatRefundId = wechatRefundId;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public Long getRefundAmountCents() {
        return refundAmountCents;
    }

    public void setRefundAmountCents(Long refundAmountCents) {
        this.refundAmountCents = refundAmountCents;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRefundPayload() {
        return refundPayload;
    }

    public void setRefundPayload(String refundPayload) {
        this.refundPayload = refundPayload;
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
