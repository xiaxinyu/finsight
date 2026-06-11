package com.finsight.domain.model;

import java.io.Serializable;

/** Category breakdown row for reports (stable code + display hierarchy). */
public class CategoryAggregate implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String name;
    private String level1Code;
    private String level1Name;
    private Double value;

    /** Legacy alias for chart labels. */
    public String getKey() {
        return name != null && !name.isBlank() ? name : code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel1Code() {
        return level1Code;
    }

    public void setLevel1Code(String level1Code) {
        this.level1Code = level1Code;
    }

    public String getLevel1Name() {
        return level1Name;
    }

    public void setLevel1Name(String level1Name) {
        this.level1Name = level1Name;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
