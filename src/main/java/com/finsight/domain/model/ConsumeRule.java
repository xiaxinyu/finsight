package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Date;

@TableName("consume_rule")
@Getter
@Setter
public class ConsumeRule extends BaseEntity {
    @TableId
    private String id;
    @TableField(value = "categoryId")
    private String categoryId;
    private String pattern;
    @TableField(value = "patternType")
    private String patternType;
    private Integer priority;
    private Integer active;
    @TableField(value = "bankCode")
    private String bankCode;
    @TableField(value = "cardTypeCode")
    private String cardTypeCode;
    private String remark;
    
    @TableField(exist = false)
    private List<String> tags;
    
    @TableField(value = "minAmount")
    private Double minAmount;
    @TableField(value = "maxAmount")
    private Double maxAmount;
    @TableField(value = "startDate")
    private Date startDate;
    @TableField(value = "endDate")
    private Date endDate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public String getPatternType() { return patternType; }
    public void setPatternType(String patternType) { this.patternType = patternType; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getActive() { return active; }
    public void setActive(Integer active) { this.active = active; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getCardTypeCode() { return cardTypeCode; }
    public void setCardTypeCode(String cardTypeCode) { this.cardTypeCode = cardTypeCode; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Double getMinAmount() { return minAmount; }
    public void setMinAmount(Double minAmount) { this.minAmount = minAmount; }
    public Double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(Double maxAmount) { this.maxAmount = maxAmount; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
}
