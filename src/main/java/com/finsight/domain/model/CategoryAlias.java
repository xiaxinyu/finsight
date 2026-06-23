package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("cls_category_alias")
@Getter
@Setter
public class CategoryAlias extends BaseEntity {
    @TableId
    private String id;
    private String categoryId;
    private String aliasCode;
    private String aliasName;
    private String reason;
}
