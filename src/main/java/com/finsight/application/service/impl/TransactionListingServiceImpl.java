package com.finsight.application.service.impl;

import com.finsight.application.service.ITransactionListingService;
import com.finsight.application.service.ITransactionService;
import com.finsight.application.service.support.ListingDateSupport;
import com.finsight.core.AppServiceException;
import com.finsight.core.DateTool;
import com.finsight.core.StringTool;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.TransactionParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class TransactionListingServiceImpl implements ITransactionListingService {

    private static final Logger log = LoggerFactory.getLogger(TransactionListingServiceImpl.class);

    @Autowired
    private ITransactionService transactionService;

    @Override
    public CollectionResult<Transaction> listTransactions(TransactionParam param) throws AppServiceException {
        Transaction transaction = buildQuery(param);
        Page page = new Page(param.getPage(), param.getRows());

        CollectionResult<Transaction> result = new CollectionResult<>();
        StopWatch stopWatch = new StopWatch("消费数据查询统计");
        stopWatch.start("查询列表数据");
        result.setRows(transactionService.getTransactions(transaction, page));
        enrichTransactionDateTime(result.getRows());
        stopWatch.stop();

        stopWatch.start("查询统计数据");
        result.setTotal(transactionService.countTransaction(transaction));
        stopWatch.stop();

        log.info("耗时打印：{}", stopWatch.prettyPrint());
        return result;
    }

    private static void enrichTransactionDateTime(List<Transaction> rows) {
        if (rows == null) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (Transaction t : rows) {
            try {
                String ds = t.getTransactionDate() == null ? "" : sdf.format(t.getTransactionDate());
                String ts = StringUtils.trimToEmpty(t.getTransactionTime());
                t.setTransactionDateTime(StringUtils.isNotBlank(ts) ? (ds + " " + ts) : ds);
            } catch (Exception ignore) {
                // keep row as-is
            }
        }
    }

    private static Transaction buildQuery(TransactionParam param) throws AppServiceException {
        Transaction transaction = new Transaction();
        if (!StringTool.isNullOrEmpty(param.getTransactionDateStartStr())) {
            transaction.setTransactionDateStart(
                    ListingDateSupport.parseMmDdYyyy(param.getTransactionDateStartStr()));
        }
        if (!StringTool.isNullOrEmpty(param.getTransactionDateEndStr())) {
            transaction.setTransactionDateEnd(
                    ListingDateSupport.parseMmDdYyyy(param.getTransactionDateEndStr()));
        }
        if (!StringTool.isNullOrEmpty(param.getConsumptionType())) {
            transaction.setConsumptionType(StringTool.changeObjToInt(StringUtils.trim(param.getConsumptionType())));
        }
        if (!StringTool.isNullOrEmpty(param.getCardTypeName())) {
            transaction.setCardTypeName(StringUtils.trim(param.getCardTypeName()));
        }
        if (!StringTool.isNullOrEmpty(param.getCardId())) {
            transaction.setBankCardId(StringUtils.trim(param.getCardId()));
        }
        if (!StringTool.isNullOrEmpty(param.getConsumeName())) {
            transaction.setConsumeName(StringUtils.trim(param.getConsumeName()));
        }
        if (!StringTool.isNullOrEmpty(param.getConsumeID())) {
            transaction.setConsumes(param.getConsumeID().split(","));
        }
        if (!StringTool.isNullOrEmpty(param.getDemoArea())) {
            transaction.setDemoArea(StringUtils.trim(param.getDemoArea()));
        }
        if (!StringTool.isNullOrEmpty(param.getWeekName())) {
            transaction.setWeekName(StringUtils.trim(param.getWeekName()));
        }
        if (!StringTool.isNullOrEmpty(param.getYear())) {
            transaction.setYear(StringUtils.trim(param.getYear()));
        }
        if (!StringTool.isNullOrEmpty(param.getMonth())) {
            transaction.setMonth(DateTool.getMonthCode(StringUtils.trim(param.getMonth())));
        }
        if (!StringTool.isNullOrEmpty(param.getTxnTypes())) {
            transaction.setTxnTypes(StringUtils.trim(param.getTxnTypes()));
        }
        if (!StringTool.isNullOrEmpty(param.getEmptyConsume())) {
            String v = StringUtils.trim(param.getEmptyConsume());
            if ("1".equals(v) || "true".equalsIgnoreCase(v)) {
                transaction.setEmptyConsume(Boolean.TRUE);
            }
        }
        return transaction;
    }
}
