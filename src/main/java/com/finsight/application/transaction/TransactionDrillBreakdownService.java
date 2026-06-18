package com.finsight.application.transaction;

import com.finsight.application.query.TransactionQuery;
import com.finsight.application.query.TransactionQueryAssembler;
import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.DrillBreakdownItem;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.infrastructure.mapper.TransactionMapper;
import com.finsight.web.api.dto.TransactionParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TransactionDrillBreakdownService {

    private static final int MAX_BREAKDOWN_ROWS = 100;
    private static final int MAX_SAMPLE_LIMIT = 500;

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionQuerySupport querySupport;

    public TransactionDrillBreakdownService(TransactionRepository transactionRepository,
                                              TransactionMapper transactionMapper,
                                              TransactionQuerySupport querySupport) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.querySupport = querySupport;
    }

    public DrillBreakdownResult load(TransactionParam param, int sampleLimit) throws AppServiceException {
        TransactionQuery query = TransactionQueryAssembler.from(param);
        applyMerchantToken(param, query);
        querySupport.enrich(query);

        int boundedSample = Math.max(1, Math.min(sampleLimit, MAX_SAMPLE_LIMIT));
        int total = transactionRepository.countTransaction(query);
        double aggregateTotal = resolveAggregateTotal(query);

        List<DrillBreakdownItem> categories = transactionMapper.drillCategoryBreakdown(query, MAX_BREAKDOWN_ROWS);
        List<DrillBreakdownItem> merchants = transactionMapper.drillMerchantBreakdown(query, MAX_BREAKDOWN_ROWS);

        Page page = new Page(1, boundedSample);
        List<Transaction> rows = transactionRepository.getTransactions(query, page);

        DrillBreakdownResult result = new DrillBreakdownResult();
        result.setTotal(total);
        result.setSampleSize(rows == null ? 0 : rows.size());
        result.setTruncated(total > result.getSampleSize());
        result.setAggregateTotal(aggregateTotal);
        result.setCategories(categories);
        result.setMerchants(merchants);
        result.setTransactions(rows);
        return result;
    }

    private void applyMerchantToken(TransactionParam param, TransactionQuery query) {
        if (param == null || StringUtils.isBlank(param.getMerchantToken())) {
            return;
        }
        query.setMerchantToken(StringUtils.trim(param.getMerchantToken()));
    }

    private double resolveAggregateTotal(TransactionQuery query) {
        Map<String, Object> stats = transactionMapper.aggregateStats(query);
        if (stats == null) {
            return 0.0;
        }
        String txnTypes = query.getTxnTypes() == null ? "" : query.getTxnTypes().trim().toLowerCase();
        if ("income".equals(txnTypes)) {
            return toDouble(stats.get("income"));
        }
        return toDouble(stats.get("expense"));
    }

    private static double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        return ((Number) value).doubleValue();
    }
}
