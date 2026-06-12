package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.config.FinsightFeatureProperties;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * When {@code finsight.metrics.reconcile-gate=true}, verifies stored metrics match report SQL
 * before advisor layers trust {@code fin_metric_monthly}.
 */
@Service
public class MetricGateService {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    private final FinsightFeatureProperties features;
    private final MetricReconciliationService reconciliationService;
    private final AuthenticationFacade authenticationFacade;

    public MetricGateService(FinsightFeatureProperties features,
                             MetricReconciliationService reconciliationService,
                             AuthenticationFacade authenticationFacade) {
        this.features = features;
        this.reconciliationService = reconciliationService;
        this.authenticationFacade = authenticationFacade;
    }

    public Map<String, Object> status(int monthsToCheck) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("gateEnabled", features.getMetrics().isReconcileGate());
        if (!features.getMetrics().isReconcileGate()) {
            out.put("ok", true);
            out.put("mismatches", List.of());
            return out;
        }
        String userId = userKey();
        List<String> mismatches = new ArrayList<>();
        YearMonth cursor = YearMonth.now();
        for (int i = 0; i < monthsToCheck; i++) {
            String ym = cursor.format(YM);
            Map<String, Object> row = reconciliationService.reconcile(userId, ym);
            if (!Boolean.TRUE.equals(row.get("ok"))) {
                @SuppressWarnings("unchecked")
                List<String> mm = (List<String>) row.getOrDefault("mismatches", List.of());
                mismatches.addAll(mm);
            }
            cursor = cursor.minusMonths(1);
        }
        out.put("ok", mismatches.isEmpty());
        out.put("mismatches", mismatches);
        return out;
    }

    public boolean useReportFallback() throws Exception {
        if (!features.getMetrics().isReconcileGate()) {
            return false;
        }
        Map<String, Object> st = status(3);
        return !Boolean.TRUE.equals(st.get("ok"));
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}
