package com.stufamily.backend.family.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

@TableName("family_group")
public class FamilyGroupDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("group_no")
    private String groupNo;
    @TableField("source_order_id")
    private Long sourceOrderId;
    @TableField("owner_user_id")
    private Long ownerUserId;
    @TableField("family_card_product_id")
    private Long familyCardProductId;
    @TableField("family_card_plan_id")
    private Long familyCardPlanId;
    @TableField("max_members")
    private Integer maxMembers;
    @TableField("current_members")
    private Integer currentMembers;
    @TableField("status")
    private String status;
    @TableField("activated_at")
    private OffsetDateTime activatedAt;
    @TableField("expire_at")
    private OffsetDateTime expireAt;
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

    public String getGroupNo() {
        return groupNo;
    }

    public void setGroupNo(String groupNo) {
        this.groupNo = groupNo;
    }

    public Long getSourceOrderId() {
        return sourceOrderId;
    }

    public void setSourceOrderId(Long sourceOrderId) {
        this.sourceOrderId = sourceOrderId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Long getFamilyCardProductId() {
        return familyCardProductId;
    }

    public void setFamilyCardProductId(Long familyCardProductId) {
        this.familyCardProductId = familyCardProductId;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public Long getFamilyCardPlanId() {
        return familyCardPlanId;
    }

    public void setFamilyCardPlanId(Long familyCardPlanId) {
        this.familyCardPlanId = familyCardPlanId;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }

    public Integer getCurrentMembers() {
        return currentMembers;
    }

    public void setCurrentMembers(Integer currentMembers) {
        this.currentMembers = currentMembers;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(OffsetDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public OffsetDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(OffsetDateTime expireAt) {
        this.expireAt = expireAt;
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
