package com.finsight.application.transaction;

import com.finsight.application.query.TransactionQuery;
import com.finsight.application.query.TransactionQueryAssembler;
import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.common.exception.AppServiceException;
import com.finsight.infrastructure.mapper.TransactionMapper;
import com.finsight.web.api.dto.TransactionParam;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TransactionStatsService {

    private final TransactionMapper transactionMapper;
    private final TransactionQuerySupport querySupport;

    public TransactionStatsService(TransactionMapper transactionMapper, TransactionQuerySupport querySupport) {
        this.transactionMapper = transactionMapper;
        this.querySupport = querySupport;
    }

    public Map<String, Object> aggregate(TransactionParam param) throws AppServiceException {
        TransactionQuery q = TransactionQueryAssembler.from(param);
        querySupport.enrich(q);
        Map<String, Object> raw = transactionMapper.aggregateStats(q);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", intVal(raw, "total"));
        out.put("income", dblVal(raw, "income"));
        out.put("expense", dblVal(raw, "expense"));
        out.put("net", dblVal(raw, "income") - dblVal(raw, "expense"));
        out.put("transfers", intVal(raw, "transfers"));
        out.put("unclassified", intVal(raw, "unclassified"));
        out.put("truncated", false);
        return out;
    }

    private static int intVal(Map<String, Object> m, String key) {
        if (m == null || m.get(key) == null) {
            return 0;
        }
        return ((Number) m.get(key)).intValue();
    }

    private static double dblVal(Map<String, Object> m, String key) {
        if (m == null || m.get(key) == null) {
            return 0;
        }
        return ((Number) m.get(key)).doubleValue();
    }
}
