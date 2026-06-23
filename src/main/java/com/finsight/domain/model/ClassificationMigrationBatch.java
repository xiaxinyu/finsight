package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@TableName("classification_migration_batch")
@Getter
@Setter
public class ClassificationMigrationBatch extends BaseEntity {
    @TableId
    private String id;
    private String batchType;
    private String status;
    private String reason;
    private Integer rowCount;
    private Date appliedAt;
}
