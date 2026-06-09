package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("budget_line")
public class BudgetLine extends BaseEntity {
    @TableId
    private String id;
    private String budgetId;
    private String categoryCode;
    private String bucketKey;
    private BigDecimal limitAmount;
    private Integer rollover;
}
