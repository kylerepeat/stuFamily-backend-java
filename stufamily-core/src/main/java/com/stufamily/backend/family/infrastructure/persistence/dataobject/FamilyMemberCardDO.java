package com.stufamily.backend.family.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@TableName("family_member_card")
public class FamilyMemberCardDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("group_id")
    private Long groupId;
    @TableField("member_no")
    private String memberNo;
    @TableField("member_name")
    private String memberName;
    @TableField("student_or_card_no")
    private String studentOrCardNo;
    @TableField("phone")
    private String phone;
    @TableField("card_received_date")
    private LocalDate cardReceivedDate;
    @TableField("added_by_user_id")
    private Long addedByUserId;
    @TableField("status")
    private String status;
    @TableField("joined_at")
    private OffsetDateTime joinedAt;
    @TableField("cancelled_at")
    private OffsetDateTime cancelledAt;
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

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

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

    public LocalDate getCardReceivedDate() {
        return cardReceivedDate;
    }

    public void setCardReceivedDate(LocalDate cardReceivedDate) {
        this.cardReceivedDate = cardReceivedDate;
    }

    public Long getAddedByUserId() {
        return addedByUserId;
    }

    public void setAddedByUserId(Long addedByUserId) {
        this.addedByUserId = addedByUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(OffsetDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
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
