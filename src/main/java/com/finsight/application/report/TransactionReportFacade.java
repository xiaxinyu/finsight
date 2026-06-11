package com.finsight.application.report;

import com.alibaba.fastjson.JSONArray;
import com.finsight.application.query.TransactionQuery;
import com.finsight.application.query.TransactionQueryAssembler;
import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.application.support.ListingDateSupport;
import com.finsight.application.transaction.ITransactionService;
import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.web.api.dto.TransactionParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Date;

@Service
public class TransactionReportFacade {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private TransactionQuerySupport transactionQuerySupport;

    public String consumeReportJson(TransactionParam param) throws AppServiceException {
        TransactionQuery q = buildQuery(param);
        return JSONArray.toJSONString(transactionRepository.consumeReport(q));
    }

    public String weekConsumeReportJson(TransactionParam param) throws AppServiceException {
        TransactionQuery q = buildQuery(param);
        return JSONArray.toJSONString(transactionRepository.weekConsumeReport(q));
    }

    public String monthConsumeReportJson(TransactionParam param) throws AppServiceException {
        TransactionQuery q = buildQuery(param);
        return JSONArray.toJSONString(transactionRepository.monthConsumeReport(q));
    }

    public String monthIncomeReportJson(TransactionParam param) throws AppServiceException {
        TransactionQuery q = buildQuery(param);
        return JSONArray.toJSONString(transactionRepository.monthIncomeReport(q));
    }

    public String monthExpenseReportJson(TransactionParam param) throws AppServiceException {
        TransactionQuery q = buildQuery(param);
        return JSONArray.toJSONString(transactionRepository.monthExpenseReport(q));
    }

    private TransactionQuery buildQuery(TransactionParam param) throws AppServiceException {
        TransactionQuery q = TransactionQueryAssembler.from(param);
        transactionQuerySupport.enrich(q);
        return q;
    }

    /**
     * Dashboard KPI JSON; validates year like the former controller.
     */
    public String homeSummary(String year, String startStr, String endStr) throws AppServiceException {
        Integer y;
        if (year == null || year.trim().isEmpty()) {
            y = LocalDate.now().getYear();
        } else {
            try {
                y = Integer.parseInt(year.trim());
            } catch (NumberFormatException ex) {
                throw new AppServiceException("year must be a valid integer");
            }
        }
        int currentYear = LocalDate.now().getYear();
        if (y < 2000 || y > currentYear + 1) {
            throw new AppServiceException("year out of range");
        }
        Date rangeStart = null;
        Date rangeEnd = null;
        if (StringUtils.isNotBlank(startStr) && StringUtils.isNotBlank(endStr)) {
            Date[] range = ListingDateSupport.parseMmDdYyyyOrDefaultOneYear(startStr, endStr);
            rangeStart = range[0];
            rangeEnd = range[1];
        }
        return transactionService.homeSummary(y, rangeStart, rangeEnd);
    }
}
