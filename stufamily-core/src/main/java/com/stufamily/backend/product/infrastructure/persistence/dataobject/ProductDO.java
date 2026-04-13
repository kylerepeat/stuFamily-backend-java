package com.stufamily.backend.product.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

@TableName("product")
public class ProductDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("product_no")
    private String productNo;
    @TableField("product_type")
    private String productType;
    @TableField("title")
    private String title;
    @TableField("subtitle")
    private String subtitle;
    @TableField("detail_content")
    private String detailContent;
    @TableField("image_urls")
    private String imageUrls;
    @TableField("contact_name")
    private String contactName;
    @TableField("contact_phone")
    private String contactPhone;
    @TableField("publish_status")
    private String publishStatus;
    @TableField("is_deleted")
    private Boolean deleted;
    @TableField("is_top")
    private Boolean top;
    @TableField("display_priority")
    private Integer displayPriority;
    @TableField("sale_start_at")
    private OffsetDateTime saleStartAt;
    @TableField("sale_end_at")
    private OffsetDateTime saleEndAt;
    @TableField("service_start_at")
    private OffsetDateTime serviceStartAt;
    @TableField("service_end_at")
    private OffsetDateTime serviceEndAt;
    @TableField("list_visibility_rule_id")
    private Long listVisibilityRuleId;
    @TableField("detail_visibility_rule_id")
    private Long detailVisibilityRuleId;
    @TableField("category_id")
    private Long categoryId;
    @TableField("created_by")
    private Long createdBy;
    @TableField("updated_by")
    private Long updatedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductNo() {
        return productNo;
    }

    public void setProductNo(String productNo) {
        this.productNo = productNo;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetailContent() {
        return detailContent;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public void setDetailContent(String detailContent) {
        this.detailContent = detailContent;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus) {
        this.publishStatus = publishStatus;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean getTop() {
        return top;
    }

    public void setTop(Boolean top) {
        this.top = top;
    }

    public Integer getDisplayPriority() {
        return displayPriority;
    }

    public void setDisplayPriority(Integer displayPriority) {
        this.displayPriority = displayPriority;
    }

    public OffsetDateTime getSaleStartAt() {
        return saleStartAt;
    }

    public void setSaleStartAt(OffsetDateTime saleStartAt) {
        this.saleStartAt = saleStartAt;
    }

    public OffsetDateTime getSaleEndAt() {
        return saleEndAt;
    }

    public void setSaleEndAt(OffsetDateTime saleEndAt) {
        this.saleEndAt = saleEndAt;
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

    public Long getListVisibilityRuleId() {
        return listVisibilityRuleId;
    }

    public void setListVisibilityRuleId(Long listVisibilityRuleId) {
        this.listVisibilityRuleId = listVisibilityRuleId;
    }

    public Long getDetailVisibilityRuleId() {
        return detailVisibilityRuleId;
    }

    public void setDetailVisibilityRuleId(Long detailVisibilityRuleId) {
        this.detailVisibilityRuleId = detailVisibilityRuleId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
