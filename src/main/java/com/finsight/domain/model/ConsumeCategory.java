package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("cls_category")
@Getter
@Setter
public class ConsumeCategory extends BaseEntity {
    @TableId
    private String id;
    private String parentId;
    private String code;
    private String name;
    private Integer level;
    private Integer sortNo;
    private Integer deleted;
    @TableField(value = "txn_types")
    private String txnTypes;

    public String getParentId() { return parentId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getLevel() { return level; }
    public Integer getSortNo() { return sortNo; }
    public String getId() { return id; }
    public Integer getDeleted() { return deleted; }
    public String getTxnTypes() { return txnTypes; }
    public void setLevel(Integer level) { this.level = level; }
    public void setId(String id) { this.id = id; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public void setTxnTypes(String txnTypes) { this.txnTypes = txnTypes; }
}
