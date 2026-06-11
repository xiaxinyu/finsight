package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Uploaded bank statement (import batch).
 */
@TableName("statement")
public class Statement extends Base {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private String content;

    @TableField("row_count")
    private Integer itemCount;

    private String status;

    @TableField("source_bank_code")
    private String sourceBankCode;
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

    /** Legacy alias for import source bank code. */
    public String getSource() {
        return sourceBankCode;
    }

    public void setSource(String source) {
        this.sourceBankCode = source;
    }

    public String getSourceBankCode() {
        return sourceBankCode;
    }

    public void setSourceBankCode(String sourceBankCode) {
        this.sourceBankCode = sourceBankCode;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
