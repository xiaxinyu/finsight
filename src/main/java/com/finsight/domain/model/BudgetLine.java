package com.finsight.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** In-memory planning POJO ({@link com.finsight.application.finance.PlanningPreferencesStore}). */
@Getter
@Setter
public class BudgetLine extends BaseEntity {
    private String id;
    private String budgetId;
    private String categoryCode;
    private String bucketKey;
    private BigDecimal limitAmount;
    private Integer rollover;
}
