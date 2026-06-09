package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("bill")
public class Bill extends BaseEntity {
    @TableId
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
