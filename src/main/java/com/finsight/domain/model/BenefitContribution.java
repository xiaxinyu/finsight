package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@TableName("ben_contribution")
@Getter
@Setter
public class BenefitContribution extends AuditableEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String benefitType;
    private String unitNo;
    private String unitName;
    private String periodLabel;
    private BigDecimal payBase;
    private BigDecimal personalPay;
    private BigDecimal unitPay;
    private BigDecimal totalPay;
    private BigDecimal personalReserved;
    private String memo;
    private Integer fiscalYear;
    private Integer deleted;
}
