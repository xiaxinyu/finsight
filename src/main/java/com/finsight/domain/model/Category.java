package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * Expense/income category node ({@code cls_category}).
 */
@TableName("cls_category")
@Getter
@Setter
public class Category extends BaseEntity {
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
    @TableField(value = "report_role")
    private String reportRole;
    @TableField(value = "semantic_tag")
    private String semanticTag;
    private Integer budgetable;
    @TableField(value = "cashflow_impact")
    private String cashflowImpact;
}
