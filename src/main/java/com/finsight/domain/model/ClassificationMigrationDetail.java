package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("classification_migration_detail")
@Getter
@Setter
public class ClassificationMigrationDetail extends BaseEntity {
    @TableId
    private String id;
    private String batchId;
    private String transactionId;
    private String oldConsumeCode;
    private String newConsumeCode;
    private String oldConsumeName;
    private String newConsumeName;
    private String action;
    private String ruleId;
}
