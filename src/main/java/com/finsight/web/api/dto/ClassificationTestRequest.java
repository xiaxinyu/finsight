package com.finsight.web.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ClassificationTestRequest {
    private String narration;
    private String bankCode;
    private String cardTypeCode;
    private Double amount;
    private Date txnDate;
    private int topN = 3;
}
