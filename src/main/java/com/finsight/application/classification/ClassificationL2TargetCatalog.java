package com.finsight.application.classification;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * v1.8 §1.3 target L2 categories for Sprint 2 seeding (Issue #69).
 * Codes are insert-only; never modify existing {@code cls_category.code}.
 */
public enum ClassificationL2TargetCatalog {

    // --- Income ---
    INCOME_SALARY("INCOME-01", "工资薪金", ClassificationL1Codes.INCOME, 1, "income", "income"),
    INCOME_SIDE("INCOME-02", "副业经营", ClassificationL1Codes.INCOME, 2, "income", "income"),
    INCOME_INVEST_GAIN("INCOME-03", "投资收益", ClassificationL1Codes.INCOME, 3, "income,invest", "investment"),
    INCOME_OTHER("INCOME-99", "其他收入", ClassificationL1Codes.INCOME, 99, "income", "income"),

    // --- Fixed expense ---
    FIXED_RENT("FIXED-01", "房租/房贷", ClassificationL1Codes.FIXED, 1, "expense", "budget"),
    FIXED_UTILITIES("FIXED-02", "物业/水电燃气", ClassificationL1Codes.FIXED, 2, "expense", "budget"),
    FIXED_TELECOM("FIXED-03", "通信网络", ClassificationL1Codes.FIXED, 3, "expense", "budget"),
    FIXED_INSURANCE("FIXED-04", "保险", ClassificationL1Codes.FIXED, 4, "expense", "cashflow"),
    FIXED_SUBSCRIPTION("FIXED-05", "会员订阅", ClassificationL1Codes.FIXED, 5, "expense", "budget"),
    FIXED_EDU("FIXED-06", "教育固定缴费", ClassificationL1Codes.FIXED, 6, "expense", "budget"),
    FIXED_REPAY("FIXED-07", "固定还款", ClassificationL1Codes.FIXED, 7, "expense", "liability"),
    FIXED_OTHER("FIXED-99", "其他固定支出", ClassificationL1Codes.FIXED, 99, "expense", "budget"),

    // --- Daily life (LIVING / DAILY- prefix) ---
    DAILY_DINE_IN("DAILY-01", "餐饮堂食", ClassificationL1Codes.LIVING, 1, "expense", "budget", false),
    DAILY_DELIVERY("DAILY-02", "外卖", ClassificationL1Codes.LIVING, 2, "expense", "budget"),
    DAILY_GROCERY("DAILY-03", "超市便利", ClassificationL1Codes.LIVING, 3, "expense", "budget"),
    DAILY_HOUSEHOLD("DAILY-04", "日用品", ClassificationL1Codes.LIVING, 4, "expense", "budget"),
    DAILY_MEDICAL("DAILY-05", "医疗药品", ClassificationL1Codes.LIVING, 5, "expense", "budget"),
    DAILY_PET("DAILY-06", "宠物", ClassificationL1Codes.LIVING, 6, "expense", "budget"),
    DAILY_HOME_REPAIR("DAILY-07", "家政维修", ClassificationL1Codes.LIVING, 7, "expense", "budget"),

    // --- Shopping ---
    SHOP_APPAREL("SHOP-01", "服饰美妆", ClassificationL1Codes.SHOPPING, 1, "expense", "budget", false),
    SHOP_DIGITAL("SHOP-02", "数码家电", ClassificationL1Codes.SHOPPING, 2, "expense", "budget"),
    SHOP_HOME("SHOP-03", "家居家装", ClassificationL1Codes.SHOPPING, 3, "expense", "budget"),
    SHOP_BABY("SHOP-04", "母婴儿童", ClassificationL1Codes.SHOPPING, 4, "expense", "budget"),
    SHOP_DURABLE("SHOP-05", "大件耐用品", ClassificationL1Codes.SHOPPING, 5, "expense", "budget"),
    SHOP_ECOMMERCE("SHOP-06", "电商购物", ClassificationL1Codes.SHOPPING, 6, "expense", "budget"),

    // --- Transport (TRAVEL / TRANS- prefix) ---
    TRANSIT_PUBLIC("TRAVEL-01", "公共交通", ClassificationL1Codes.TRAVEL, 1, "expense", "budget", false),
    TRANS_RIDE("TRANS-02", "打车/网约车", ClassificationL1Codes.TRAVEL, 2, "expense", "budget"),
    TRANS_FUEL("TRANS-03", "油费/充电", ClassificationL1Codes.TRAVEL, 3, "expense", "budget"),
    TRANS_PARKING("TRANS-04", "停车", ClassificationL1Codes.TRAVEL, 4, "expense", "budget"),
    TRANS_MAINT("TRANS-05", "保养维修", ClassificationL1Codes.TRAVEL, 5, "expense", "budget"),
    TRANS_CAR_INS("TRANS-06", "车辆保险", ClassificationL1Codes.TRAVEL, 6, "expense", "cashflow"),
    TRANS_TICKET("TRANS-07", "机票/火车", ClassificationL1Codes.TRAVEL, 7, "expense", "budget"),

    // --- Entertainment & travel ---
    ENT_HOTEL("ENT-01", "酒店住宿", ClassificationL1Codes.ENT, 1, "expense", "budget"),
    ENT_TICKET("ENT-02", "景点门票", ClassificationL1Codes.ENT, 2, "expense", "budget"),
    ENT_SHOW("ENT-03", "电影演出", ClassificationL1Codes.ENT, 3, "expense", "budget"),
    ENT_GAME("ENT-04", "游戏娱乐", ClassificationL1Codes.ENT, 4, "expense", "budget"),
    ENT_TOUR("ENT-05", "旅游消费", ClassificationL1Codes.ENT, 5, "expense", "budget"),
    ENT_SPORT("ENT-06", "运动健身", ClassificationL1Codes.ENT, 6, "expense", "budget"),

    // --- Education (EDU L1) ---
    EDU_TUITION("EDU-01", "学费培训", ClassificationL1Codes.EDU, 1, "expense", "budget"),
    EDU_BOOKS("EDU-02", "书籍资料", ClassificationL1Codes.EDU, 2, "expense", "budget"),

    // --- Gift & charity ---
    GIFT_REDPACK("GIFT-01", "红包礼金", ClassificationL1Codes.GIFT, 1, "expense", "budget"),
    GIFT_TRANSFER("GIFT-02", "转账赠与", ClassificationL1Codes.GIFT, 2, "expense,transfer", "transfer"),
    GIFT_DONATE("GIFT-03", "公益捐赠", ClassificationL1Codes.GIFT, 3, "expense", "budget"),
    GIFT_FAMILY("GIFT-04", "家庭支持", ClassificationL1Codes.GIFT, 4, "expense", "budget"),

    // --- Reimbursement & refund ---
    REIM_CORP("REIM-01", "公司报销", ClassificationL1Codes.REIM, 1, "income,refund", "refund"),
    REIM_REFUND("REIM-02", "消费退款", ClassificationL1Codes.REIM, 2, "income,refund", "refund"),
    REIM_DEPOSIT("REIM-03", "押金返还", ClassificationL1Codes.REIM, 3, "income,refund", "refund"),
    REIM_CASHBACK("REIM-04", "平台返现", ClassificationL1Codes.REIM, 4, "income,refund", "refund"),
    REIM_CC_RETURN("REIM-05", "信用卡退货", ClassificationL1Codes.REIM, 5, "income,refund", "refund"),

    // --- Asset movement ---
    ASSET_ATM("ASSET-01", "ATM 取现", ClassificationL1Codes.ASSET, 1, "transfer,asset", "asset"),
    ASSET_INTERNAL("ASSET-02", "账户间转入转出", ClassificationL1Codes.ASSET, 2, "transfer", "transfer"),
    ASSET_ADJUST("ASSET-03", "余额调整", ClassificationL1Codes.ASSET, 3, "transfer,asset", "asset"),
    ASSET_SAVINGS("ASSET-04", "储蓄转入", ClassificationL1Codes.ASSET, 4, "transfer,asset", "asset"),
    ASSET_PURCHASE("ASSET-05", "资产购买", ClassificationL1Codes.ASSET, 5, "transfer,asset", "asset"),

    // --- Liability ---
    DEBT_CC_REPAY("DEBT-01", "信用卡还款", ClassificationL1Codes.LIABILITY, 1, "transfer,liability", "liability"),
    DEBT_BORROW_IN("DEBT-02", "借款收到", ClassificationL1Codes.LIABILITY, 2, "transfer,liability", "liability"),
    DEBT_LOAN_REPAY("DEBT-03", "贷款还款", ClassificationL1Codes.LIABILITY, 3, "expense,liability", "liability"),
    DEBT_INSTALL("DEBT-04", "分期还款", ClassificationL1Codes.LIABILITY, 4, "expense,liability", "liability"),
    DEBT_INTEREST("DEBT-05", "利息支出", ClassificationL1Codes.LIABILITY, 5, "expense", "cashflow"),

    // --- Investment activity (INVEST-01/02 may already exist — insertWhenMissing only) ---
    INV_FUND_BUY("INVEST-01", "基金申购", ClassificationL1Codes.INVEST, 1, "expense,invest", "investment", false),
    INV_STOCK_LEGACY("INVEST-02", "股票", ClassificationL1Codes.INVEST, 2, "expense,invest", "investment", false),
    INV_STOCK_BUY("INVEST-03", "股票买入", ClassificationL1Codes.INVEST, 3, "expense,invest", "investment", true),
    INV_STOCK_SELL("INVEST-04", "股票卖出", ClassificationL1Codes.INVEST, 4, "income,invest", "investment", true),
    INV_BROKER_XFER("INVEST-05", "证券转账", ClassificationL1Codes.INVEST, 5, "transfer,invest", "investment", true),
    INV_FUND_SELL("INVEST-06", "基金赎回", ClassificationL1Codes.INVEST, 6, "income,invest", "investment", true),

    // --- Wealth products ---
    WEALTH_BANK_BUY("WEALTH-01", "银行理财申购", ClassificationL1Codes.WEALTH, 1, "expense,invest", "investment"),
    WEALTH_BANK_SELL("WEALTH-02", "银行理财赎回", ClassificationL1Codes.WEALTH, 2, "income,invest", "investment"),
    WEALTH_MMF("WEALTH-03", "货币基金", ClassificationL1Codes.WEALTH, 3, "invest", "investment"),
    WEALTH_TD("WEALTH-04", "定期存款", ClassificationL1Codes.WEALTH, 4, "transfer,invest", "investment"),
    WEALTH_STRUCT("WEALTH-05", "结构性产品", ClassificationL1Codes.WEALTH, 5, "invest", "investment"),

    // --- Fees ---
    FEE_BANK("FEE-01", "银行手续费", ClassificationL1Codes.FEE, 1, "expense", "cashflow"),
    FEE_CC_ANNUAL("FEE-02", "信用卡年费", ClassificationL1Codes.FEE, 2, "expense", "cashflow"),
    FEE_BROKER("FEE-03", "证券手续费", ClassificationL1Codes.FEE, 3, "expense", "cashflow"),
    FEE_INSTALL("FEE-04", "分期手续费", ClassificationL1Codes.FEE, 4, "expense", "cashflow"),
    FEE_FX("FEE-05", "汇兑手续费", ClassificationL1Codes.FEE, 5, "expense", "cashflow"),

    // --- Other catch-all ---
    OTHER_TEMP("OTHER-01", "临时无法归类", ClassificationL1Codes.OTHER, 1, "expense", "budget", false),
    OTHER_LOW_FREQ("OTHER-02", "低频小额未知商户", ClassificationL1Codes.OTHER, 2, "expense", "budget"),
    OTHER_REVIEW("OTHER-03", "待人工确认", ClassificationL1Codes.OTHER, 3, "expense", "budget");

    private final String code;
    private final String name;
    private final String parentL1Code;
    private final int sortNo;
    private final String txnTypes;
    private final String reportRole;
    private final boolean insertWhenMissing;

    ClassificationL2TargetCatalog(
            String code,
            String name,
            String parentL1Code,
            int sortNo,
            String txnTypes,
            String reportRole) {
        this(code, name, parentL1Code, sortNo, txnTypes, reportRole, true);
    }

    ClassificationL2TargetCatalog(
            String code,
            String name,
            String parentL1Code,
            int sortNo,
            String txnTypes,
            String reportRole,
            boolean insertWhenMissing) {
        this.code = code;
        this.name = name;
        this.parentL1Code = parentL1Code;
        this.sortNo = sortNo;
        this.txnTypes = txnTypes;
        this.reportRole = reportRole;
        this.insertWhenMissing = insertWhenMissing;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return name;
    }

    public String parentL1Code() {
        return parentL1Code;
    }

    public int sortNo() {
        return sortNo;
    }

    public String txnTypes() {
        return txnTypes;
    }

    public String reportRole() {
        return reportRole;
    }

    /** When false, catalog entry documents an existing code — never auto-insert. */
    public boolean insertWhenMissing() {
        return insertWhenMissing;
    }

    public static List<ClassificationL2TargetCatalog> insertableBatch() {
        return Arrays.stream(values()).filter(ClassificationL2TargetCatalog::insertWhenMissing).toList();
    }

    public static Optional<ClassificationL2TargetCatalog> byCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim();
        return Arrays.stream(values())
                .filter(c -> c.code.equals(normalized))
                .findFirst();
    }
}
