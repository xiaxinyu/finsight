package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;

import java.util.Date;

/**
 * Legacy auditable superclass for benefit/statement entities.
 */
public class Base extends AuditableEntity implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /** @deprecated use {@link #getCreatedBy()} */
    @Deprecated
    public String getCreateuser() {
        return getCreatedBy();
    }

    /** @deprecated use {@link #setCreatedBy(String)} */
    @Deprecated
    public void setCreateuser(String createuser) {
        setCreatedBy(createuser);
    }

    /** @deprecated use {@link #getCreatedAt()} */
    @Deprecated
    public Date getCreatetime() {
        return getCreatedAt();
    }

    /** @deprecated use {@link #setCreatedAt(Date)} */
    @Deprecated
    public void setCreatetime(Date createtime) {
        setCreatedAt(createtime);
    }

    /** @deprecated use {@link #getUpdatedBy()} */
    @Deprecated
    public String getUpdateuser() {
        return getUpdatedBy();
    }

    /** @deprecated use {@link #setUpdatedBy(String)} */
    @Deprecated
    public void setUpdateuser(String updateuser) {
        setUpdatedBy(updateuser);
    }

    /** @deprecated use {@link #getUpdatedAt()} */
    @Deprecated
    public Date getUpdatetime() {
        return getUpdatedAt();
    }

    /** @deprecated use {@link #setUpdatedAt(Date)} */
    @Deprecated
    public void setUpdatetime(Date updatetime) {
        setUpdatedAt(updatetime);
    }
}
