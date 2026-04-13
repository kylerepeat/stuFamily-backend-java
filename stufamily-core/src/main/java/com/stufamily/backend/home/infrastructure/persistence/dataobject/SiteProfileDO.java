package com.stufamily.backend.home.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("site_profile")
public class SiteProfileDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("community_name")
    private String communityName;
    @TableField("intro_text")
    private String introText;
    @TableField("contact_person")
    private String contactPerson;
    @TableField("contact_phone")
    private String contactPhone;
    @TableField("contact_wechat")
    private String contactWechat;
    @TableField("contact_wechat_qr_url")
    private String contactWechatQrUrl;
    @TableField("banner_slogan")
    private String bannerSlogan;
    @TableField("address_text")
    private String addressText;
    @TableField("latitude")
    private BigDecimal latitude;
    @TableField("longitude")
    private BigDecimal longitude;
    @TableField("active")
    private Boolean active;
    @TableField("updated_by")
    private Long updatedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCommunityName() {
        return communityName;
    }

    public void setCommunityName(String communityName) {
        this.communityName = communityName;
    }

    public String getIntroText() {
        return introText;
    }

    public void setIntroText(String introText) {
        this.introText = introText;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactWechat() {
        return contactWechat;
    }

    public void setContactWechat(String contactWechat) {
        this.contactWechat = contactWechat;
    }

    public String getContactWechatQrUrl() {
        return contactWechatQrUrl;
    }

    public void setContactWechatQrUrl(String contactWechatQrUrl) {
        this.contactWechatQrUrl = contactWechatQrUrl;
    }

    public String getAddressText() {
        return addressText;
    }

    public String getBannerSlogan() {
        return bannerSlogan;
    }

    public void setBannerSlogan(String bannerSlogan) {
        this.bannerSlogan = bannerSlogan;
    }

    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
