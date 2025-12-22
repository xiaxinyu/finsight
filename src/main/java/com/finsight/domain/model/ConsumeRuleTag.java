package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("consume_rule_tag")
@Getter
@Setter
public class ConsumeRuleTag {
    private String ruleId;
    private String tag;

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}
