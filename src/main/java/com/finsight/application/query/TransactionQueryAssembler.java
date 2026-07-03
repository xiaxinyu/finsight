package com.finsight.application.query;

import com.finsight.application.support.ListingDateSupport;
import com.finsight.common.exception.AppServiceException;
import com.finsight.common.util.DateTool;
import com.finsight.common.util.StringTool;
import com.finsight.web.api.dto.TransactionParam;
import org.apache.commons.lang3.StringUtils;

/**
 * Maps HTTP/grid {@link TransactionParam} to {@link TransactionQuery} for repository calls.
 */
public final class TransactionQueryAssembler {

    private TransactionQueryAssembler() {
    }

    public static TransactionQuery from(TransactionParam param) throws AppServiceException {
        TransactionQuery q = new TransactionQuery();
        java.util.Date[] range = ListingDateSupport.parseMmDdYyyyOrNull(
                param.getTransactionDateStartStr(), param.getTransactionDateEndStr());
        q.setTransactionDateStart(range[0]);
        q.setTransactionDateEnd(range[1]);
        if (!StringTool.isNullOrEmpty(param.getConsumptionType())) {
            q.setConsumptionType(StringTool.changeObjToInt(StringUtils.trim(param.getConsumptionType())));
        }
        if (!StringTool.isNullOrEmpty(param.getCardTypeName())) {
            q.setCardTypeName(StringUtils.trim(param.getCardTypeName()));
        }
        if (!StringTool.isNullOrEmpty(param.getCardId())) {
            q.setBankCardId(StringUtils.trim(param.getCardId()));
        }
        if (!StringTool.isNullOrEmpty(param.getStrictCard())) {
            String v = StringUtils.trim(param.getStrictCard());
            q.setStrictBankCard("1".equals(v) || "true".equalsIgnoreCase(v));
        }
        if (!StringTool.isNullOrEmpty(param.getConsumeName())) {
            q.setConsumeName(StringUtils.trim(param.getConsumeName()));
        }
        if (!StringTool.isNullOrEmpty(param.getConsumeID())) {
            q.setConsumes(param.getConsumeID().split(","));
        }
        if (!StringTool.isNullOrEmpty(param.getDemoArea())) {
            q.setDemoArea(StringUtils.trim(param.getDemoArea()));
        }
        if (!StringTool.isNullOrEmpty(param.getWeekName())) {
            q.setWeekName(StringUtils.trim(param.getWeekName()));
        }
        if (!StringTool.isNullOrEmpty(param.getYear())) {
            q.setYear(StringUtils.trim(param.getYear()));
        }
        if (!StringTool.isNullOrEmpty(param.getMonth())) {
            q.setMonth(DateTool.getMonthCode(StringUtils.trim(param.getMonth())));
        }
        if (!StringTool.isNullOrEmpty(param.getTxnTypes())) {
            q.setTxnTypes(StringUtils.trim(param.getTxnTypes()));
        }
        if (!StringTool.isNullOrEmpty(param.getEmptyConsume())) {
            String v = StringUtils.trim(param.getEmptyConsume());
            if ("1".equals(v) || "true".equalsIgnoreCase(v)) {
                q.setEmptyConsume(Boolean.TRUE);
            }
        }
        if (!StringTool.isNullOrEmpty(param.getMerchantToken())) {
            q.setMerchantToken(StringUtils.trim(param.getMerchantToken()).toLowerCase());
        }
        if (!StringTool.isNullOrEmpty(param.getSemanticFilter())) {
            q.setSemanticFilter(StringUtils.trim(param.getSemanticFilter()));
        }
        TransactionSort.apply(param, q);
        return q;
    }
}
