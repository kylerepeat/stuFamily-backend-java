package com.stufamily.backend.family.infrastructure.persistence.dataobject;

import java.time.OffsetDateTime;

public class FamilyMemberListRowDO {
    private String memberNo;
    private String memberName;
    private String studentOrCardNo;
    private String phone;
    private OffsetDateTime joinedAt;
    private String status;
    private OffsetDateTime familyGroupExpireAt;
    private String wechatAvatarUrl;

    public String getMemberNo() {
        return memberNo;
    }

    public void setMemberNo(String memberNo) {
        this.memberNo = memberNo;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getStudentOrCardNo() {
        return studentOrCardNo;
    }

    public void setStudentOrCardNo(String studentOrCardNo) {
        this.studentOrCardNo = studentOrCardNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getFamilyGroupExpireAt() {
        return familyGroupExpireAt;
    }

    public void setFamilyGroupExpireAt(OffsetDateTime familyGroupExpireAt) {
        this.familyGroupExpireAt = familyGroupExpireAt;
    }

    public String getWechatAvatarUrl() {
        return wechatAvatarUrl;
    }

    public void setWechatAvatarUrl(String wechatAvatarUrl) {
        this.wechatAvatarUrl = wechatAvatarUrl;
    }
}

