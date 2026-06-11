package com.finsight.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/** In-memory planning POJO ({@link com.finsight.application.finance.PlanningPreferencesStore}). */
@Getter
@Setter
public class FinancialGoal extends BaseEntity {
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
