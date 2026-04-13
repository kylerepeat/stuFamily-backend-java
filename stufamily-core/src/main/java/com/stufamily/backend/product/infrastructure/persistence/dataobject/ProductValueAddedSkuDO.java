package com.stufamily.backend.product.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("product_value_added_sku")
public class ProductValueAddedSkuDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("product_id")
    private Long productId;
    @TableField("sku_no")
    private String skuNo;
    @TableField("title")
    private String title;
    @TableField("unit_name")
    private String unitName;
    @TableField("price_cents")
    private Long priceCents;
    @TableField("max_purchase_qty")
    private Integer maxPurchaseQty;
    @TableField("service_notice")
    private String serviceNotice;
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

    public String getSkuNo() {
        return skuNo;
    }

    public void setSkuNo(String skuNo) {
        this.skuNo = skuNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public Long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(Long priceCents) {
        this.priceCents = priceCents;
    }

    public Integer getMaxPurchaseQty() {
        return maxPurchaseQty;
    }

    public void setMaxPurchaseQty(Integer maxPurchaseQty) {
        this.maxPurchaseQty = maxPurchaseQty;
    }

    public String getServiceNotice() {
        return serviceNotice;
    }

    public void setServiceNotice(String serviceNotice) {
        this.serviceNotice = serviceNotice;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
