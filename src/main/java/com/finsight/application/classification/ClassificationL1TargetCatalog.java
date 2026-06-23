package com.finsight.application.classification;

import java.util.Arrays;
import java.util.List;

/**
 * L1 category roots referenced by {@link ClassificationL2TargetCatalog}.
 * After category dedup, {@code INC} and {@code TRANSPORT} are the preferred canonical roots.
 */
public enum ClassificationL1TargetCatalog {

    INC(ClassificationL1Codes.INC, "收入", 10, "income"),
    FIXED(ClassificationL1Codes.FIXED, "固定支出", 20, "expense"),
    LIVING(ClassificationL1Codes.LIVING, "日常生活", 30, "expense"),
    SHOPPING(ClassificationL1Codes.SHOPPING, "购物与耐用品", 40, "expense"),
    TRANSPORT(ClassificationL1Codes.TRANSPORT, "交通与车辆", 50, "expense"),
    EDU(ClassificationL1Codes.EDU, "教育与培训", 55, "expense"),
    ENT(ClassificationL1Codes.ENT, "娱乐与旅行", 60, "expense"),
    GIFT(ClassificationL1Codes.GIFT, "人情与公益", 65, "expense"),
    REIM(ClassificationL1Codes.REIM, "报销与返还", 70, "income,refund"),
    ASSET(ClassificationL1Codes.ASSET, "资产变动", 75, "transfer,asset"),
    LIABILITY(ClassificationL1Codes.LIABILITY, "负债变动", 80, "transfer,liability"),
    INVEST(ClassificationL1Codes.INVEST, "投资活动", 85, "expense,invest"),
    WEALTH(ClassificationL1Codes.WEALTH, "理财与金融产品", 90, "invest"),
    FEE(ClassificationL1Codes.FEE, "金融手续费", 92, "expense"),
    OTHER(ClassificationL1Codes.OTHER, "其它消费", 99, "expense");

    private final String code;
    private final String name;
    private final int sortNo;
    private final String txnTypes;

    ClassificationL1TargetCatalog(String code, String name, int sortNo, String txnTypes) {
        this.code = code;
        this.name = name;
        this.sortNo = sortNo;
        this.txnTypes = txnTypes;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return name;
    }

    public int sortNo() {
        return sortNo;
    }

    public String txnTypes() {
        return txnTypes;
    }

    public static List<ClassificationL1TargetCatalog> all() {
        return Arrays.asList(values());
    }
}
