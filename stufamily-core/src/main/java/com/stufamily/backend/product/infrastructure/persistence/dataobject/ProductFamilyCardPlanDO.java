package com.stufamily.backend.product.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("product_family_card_plan")
public class ProductFamilyCardPlanDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("product_id")
    private Long productId;
    @TableField("duration_type")
    private String durationType;
    @TableField("duration_months")
    private Integer durationMonths;
    @TableField("price_cents")
    private Long priceCents;
    @TableField("max_family_members")
    private Integer maxFamilyMembers;
    @TableField("enabled")
    private Boolean enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getDurationType() {
        return durationType;
    }

    public void setDurationType(String durationType) {
        this.durationType = durationType;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public Long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(Long priceCents) {
        this.priceCents = priceCents;
    }

    public Integer getMaxFamilyMembers() {
        return maxFamilyMembers;
    }

    public void setMaxFamilyMembers(Integer maxFamilyMembers) {
        this.maxFamilyMembers = maxFamilyMembers;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}

