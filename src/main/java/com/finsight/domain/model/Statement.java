package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;

/**
 * Statement Entity (formerly TransactionRecord)
 * Represents an uploaded bank statement or bill file.
 */
@TableName("statement")
public class Statement extends Base {
    private static final long serialVersionUID = 1L;

    @TableField("bill_file_name")
    private String fileName;

    @TableField("bill_data")
    private String content;

    @TableField(value = "bill_items_count", exist = false)
    private Integer itemCount;

    @TableField(exist = false)
    private String status; // UPLOADED, PROCESSED, ERROR

    @TableField(exist = false)
    private String source; // e.g. "CCB", "CMB"

    @TableField(exist = false)
    private Integer deleted;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
