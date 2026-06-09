package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("budget")
public class Budget extends BaseEntity {
    @TableId
    private String id;
    private String name;
    private String periodType;
    private Integer year;
    private Integer month;
    private Integer deleted;
}
