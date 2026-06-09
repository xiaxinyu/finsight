package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("financial_account")
public class FinancialAccount extends BaseEntity {
    @TableId
    private String id;
    private String name;
    private String accountType;
    private String bankCardId;
    private String currency;
    private Integer isLiability;
    private Integer displayOrder;
    private Integer deleted;
}
