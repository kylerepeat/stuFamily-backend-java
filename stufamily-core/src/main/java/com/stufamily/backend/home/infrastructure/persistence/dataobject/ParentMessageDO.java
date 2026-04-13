package com.stufamily.backend.home.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

@TableName("parent_message")
public class ParentMessageDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("parent_id")
    private Long parentId;
    @TableField("root_id")
    private Long rootId;
    @TableField("sender_type")
    private String senderType;
    @TableField("nickname_snapshot")
    private String nicknameSnapshot;
    @TableField("avatar_snapshot")
    private String avatarSnapshot;
    @TableField("content")
    private String content;
    @TableField("status")
    private String status;
    @TableField("viewed")
    private Boolean viewed;
    @TableField("viewed_at")
    private OffsetDateTime viewedAt;
    @TableField("replied_at")
    private OffsetDateTime repliedAt;
    @TableField("closed")
    private Boolean closed;
    @TableField("deleted")
    private Boolean deleted;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getRootId() {
        return rootId;
    }

    public void setRootId(Long rootId) {
        this.rootId = rootId;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getNicknameSnapshot() {
        return nicknameSnapshot;
    }

    public void setNicknameSnapshot(String nicknameSnapshot) {
        this.nicknameSnapshot = nicknameSnapshot;
    }

    public String getAvatarSnapshot() {
        return avatarSnapshot;
    }

    public void setAvatarSnapshot(String avatarSnapshot) {
        this.avatarSnapshot = avatarSnapshot;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getViewed() {
        return viewed;
    }

    public void setViewed(Boolean viewed) {
        this.viewed = viewed;
    }

    public OffsetDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(OffsetDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }

    public OffsetDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(OffsetDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
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
