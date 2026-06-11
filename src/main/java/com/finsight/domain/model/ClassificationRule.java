package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * Keyword rule for auto-classification ({@code cls_rule}).
 */
@TableName("cls_rule")
@Getter
@Setter
public class ClassificationRule extends BaseEntity {
    @TableId
    private String id;
    private String categoryId;
    private String pattern;
    private String patternType;
    private Integer priority;
    private Integer active;
    private String bankCode;
    private String cardTypeCode;
    private String remark;

    @TableField(exist = false)
    private List<String> tags;

    private Double minAmount;
    private Double maxAmount;
    private Date startDate;
    private Date endDate;
}
