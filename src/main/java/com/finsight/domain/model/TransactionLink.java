package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("transaction_link")
public class TransactionLink extends BaseEntity {
    @TableId
    private String id;
    private String fromTransactionId;
    private String toTransactionId;
    private String linkType;
    private BigDecimal confidence;
}
