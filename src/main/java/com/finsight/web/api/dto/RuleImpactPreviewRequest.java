package com.finsight.web.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class RuleImpactPreviewRequest {
    private String ruleId;
    private String pattern;
    private String patternType;
    private String categoryId;
    private Integer priority;
    private String bankCode;
    private String cardTypeCode;
    private Double minAmount;
    private Double maxAmount;
    private Date startDate;
    private Date endDate;
    /** UNCLASSIFIED_ONLY | WOULD_OVERRIDE | ALL_MATCHES */
    private String scope;
}
