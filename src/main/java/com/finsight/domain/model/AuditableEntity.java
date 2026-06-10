package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Standard audit columns (DB: created_at, created_by, updated_at, updated_by).
 */
@Getter
@Setter
public class AuditableEntity {

    @TableField(exist = false)
    private Integer version = 0;

    private String createdBy;
    private Date createdAt;
    private String updatedBy;
    private Date updatedAt;

    /** @deprecated use {@link #getCreatedBy()} */
    @Deprecated
    public String getCreateUser() {
        return createdBy;
    }

    /** @deprecated use {@link #setCreatedBy(String)} */
    @Deprecated
    public void setCreateUser(String createUser) {
        this.createdBy = createUser;
    }

    /** @deprecated use {@link #getUpdatedBy()} */
    @Deprecated
    public String getUpdateUser() {
        return updatedBy;
    }

    /** @deprecated use {@link #setUpdatedBy(String)} */
    @Deprecated
    public void setUpdateUser(String updateUser) {
        this.updatedBy = updateUser;
    }

    /** @deprecated use {@link #getCreatedAt()} */
    @Deprecated
    public Date getCreateTime() {
        return createdAt;
    }

    /** @deprecated use {@link #setCreatedAt(Date)} */
    @Deprecated
    public void setCreateTime(Date createTime) {
        this.createdAt = createTime;
    }

    /** @deprecated use {@link #getUpdatedAt()} */
    @Deprecated
    public Date getUpdateTime() {
        return updatedAt;
    }

    /** @deprecated use {@link #setUpdatedAt(Date)} */
    @Deprecated
    public void setUpdateTime(Date updateTime) {
        this.updatedAt = updateTime;
    }
}
