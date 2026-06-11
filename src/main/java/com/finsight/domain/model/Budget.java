package com.finsight.domain.model;

import lombok.Getter;
import lombok.Setter;

/** In-memory planning POJO ({@link com.finsight.application.finance.PlanningPreferencesStore}). */
@Getter
@Setter
public class Budget extends BaseEntity {
    private String id;
    private String name;
    private String periodType;
    private Integer year;
    private Integer month;
    private Integer deleted;
}
