package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.query.TransactionQuery;
import com.finsight.application.query.TransactionQueryAssembler;
import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.web.api.dto.TransactionParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrendAnalysisService {

    private final TransactionRepository transactionRepository;
    private final TransactionQuerySupport querySupport;
    private final AuthenticationFacade authenticationFacade;

    public TrendAnalysisService(TransactionRepository transactionRepository,
                                TransactionQuerySupport querySupport,
                                AuthenticationFacade authenticationFacade) {
        this.transactionRepository = transactionRepository;
        this.querySupport = querySupport;
        this.authenticationFacade = authenticationFacade;
    }

    public Map<String, Object> trends(int fromYear, int toYear) throws Exception {
        List<Map<String, Object>> categoryShifts = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            final int year = y;
            TransactionParam param = new TransactionParam();
            param.setTransactionDateStartStr("01/01/" + year);
            param.setTransactionDateEndStr("12/31/" + year);
            param.setTxnTypes("expense");
            TransactionQuery q = TransactionQueryAssembler.from(param);
            querySupport.enrich(q);
            transactionRepository.consumeReport(q).forEach(cat -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("year", year);
                row.put("categoryCode", cat.getCode());
                row.put("categoryName", cat.getName());
                row.put("amount", cat.getValue());
                categoryShifts.add(row);
            });
        }

        List<Map<String, Object>> topGrowth = topCategoryGrowth(categoryShifts, fromYear, toYear);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fromYear", fromYear);
        out.put("toYear", toYear);
        out.put("topCategoryGrowth", topGrowth);
        out.put("savingsInflection", savingsInflection(fromYear, toYear));
        out.put("user", userKey());
        return out;
    }

    private List<Map<String, Object>> topCategoryGrowth(List<Map<String, Object>> rows, int fromYear, int toYear) {
        Map<String, Double> start = new LinkedHashMap<>();
        Map<String, Double> end = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            int year = ((Number) row.get("year")).intValue();
            String code = String.valueOf(row.get("categoryCode"));
            double amt = ((Number) row.get("amount")).doubleValue();
            if (year == fromYear) {
                start.merge(code, amt, Double::sum);
            }
            if (year == toYear) {
                end.merge(code, amt, Double::sum);
            }
        }
        List<Map<String, Object>> growth = new ArrayList<>();
        for (String code : end.keySet()) {
            double s = start.getOrDefault(code, 0.0);
            double e = end.getOrDefault(code, 0.0);
            if (s <= 0 && e <= 0) {
                continue;
            }
            double pct = s > 0 ? (e - s) / s * 100 : 100;
            if (Math.abs(pct) >= 10 && Math.abs(e - s) >= 100) {
                Map<String, Object> g = new LinkedHashMap<>();
                g.put("categoryCode", code);
                g.put("pctChange", Math.round(pct));
                g.put("deltaAmount", e - s);
                growth.add(g);
            }
        }
        growth.sort(Comparator.comparingDouble(g -> -Math.abs(((Number) g.get("pctChange")).doubleValue())));
        return growth.size() > 5 ? growth.subList(0, 5) : growth;
    }

    private Map<String, Object> savingsInflection(int fromYear, int toYear) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("note", "Compare net cashflow between " + fromYear + " and " + toYear);
        m.put("fromYear", fromYear);
        m.put("toYear", toYear);
        return m;
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}
