package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@TableName("transfer_pair")
public class TransferPair extends BaseEntity {
    @TableId
    private String id;
    private String fromAccountId;
    private String toAccountId;
    private String fromTransactionId;
    private String toTransactionId;
    private BigDecimal amount;
    private Date transferDate;
    private String transferGroupId;
    private String memo;
    private Integer deleted;
}
