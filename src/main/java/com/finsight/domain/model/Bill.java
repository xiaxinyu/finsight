package com.finsight.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** In-memory planning POJO ({@link com.finsight.application.finance.PlanningPreferencesStore}). */
@Getter
@Setter
public class Bill extends BaseEntity {
    private String id;
    private String name;
    private BigDecimal amount;
    private Integer dueDay;
    private String recurrence;
    private String accountId;
    private String categoryCode;
    private Integer enabled;
    private Integer deleted;
}
