package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@TableName("financial_goal")
public class FinancialGoal extends BaseEntity {
    @TableId
    private String id;
    private String name;
    private String goalType;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private Date targetDate;
    private BigDecimal monthlyContribution;
    private String linkedAccountId;
    private Integer deleted;
}
