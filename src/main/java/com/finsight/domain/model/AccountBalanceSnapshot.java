package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@TableName("account_balance_snapshot")
public class AccountBalanceSnapshot extends BaseEntity {
    @TableId
    private String id;
    private String accountId;
    private Date snapshotDate;
    private BigDecimal balance;
    private String source;
}
