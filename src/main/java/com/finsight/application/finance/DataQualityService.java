package com.finsight.application.finance;

import com.finsight.application.analytics.MetricGateService;
import com.finsight.application.authentication.LedgerUserScope;
import com.finsight.application.classification.ConfigVersionService;
import com.finsight.application.config.FinsightFeatureProperties;
import com.finsight.infrastructure.mapper.DataQualityMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DataQualityService {

    private final UserScopedFinancialQueries scopedFinancialQueries;
    private final DataQualityMapper dataQualityMapper;
    private final ConfigVersionService configVersionService;
    private final LedgerUserScope ledgerUserScope;
    private final MetricGateService metricGateService;
    private final FinsightFeatureProperties features;

    public DataQualityService(UserScopedFinancialQueries scopedFinancialQueries,
                              DataQualityMapper dataQualityMapper,
                              ConfigVersionService configVersionService,
                              LedgerUserScope ledgerUserScope,
                              MetricGateService metricGateService,
                              FinsightFeatureProperties features) {
        this.scopedFinancialQueries = scopedFinancialQueries;
        this.dataQualityMapper = dataQualityMapper;
        this.configVersionService = configVersionService;
        this.ledgerUserScope = ledgerUserScope;
        this.metricGateService = metricGateService;
        this.features = features;
    }

    public Map<String, Object> summary() {
        String owner = ledgerUserScope.resolve();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("unclassifiedCount", scopedFinancialQueries.countUnclassified());
        m.put("transferPairCount", scopedFinancialQueries.countTransferGroups());
        putCoverage(m, owner);
        m.put("orphanCategoryTxnCount", safeOrphanCount(owner));
        m.put("refundExcludedCount", safeRefundCount(owner));
        putMerchantCoverage(m, owner);
        m.put("versions", configVersionService.asMap());
        return m;
    }

    public Map<String, Object> reportStrip(String metricsSource) {
        Map<String, Object> strip = summary();
        strip.put("metricsSource", metricsSource == null ? "fin_metric_monthly" : metricsSource);
        strip.put("confidence", confidenceLevel(strip));
        strip.put("reconcileGateEnabled", features.getMetrics().isReconcileGate());
        if (features.getMetrics().isReconcileGate()) {
            try {
                strip.put("metricsGate", metricGateService.status(3));
            } catch (Exception e) {
                Map<String, Object> gate = new LinkedHashMap<>();
                gate.put("gateEnabled", true);
                gate.put("ok", false);
                gate.put("warning", "Metrics reconcile check failed: " + e.getMessage());
                strip.put("metricsGate", gate);
            }
        }
        return strip;
    }

    private void putCoverage(Map<String, Object> m, String owner) {
        try {
            Map<String, Object> cov = dataQualityMapper.classificationCoverage(owner);
            if (cov != null) {
                m.put("totalTxnCount", cov.get("totalTxns"));
                m.put("unclassifiedPct", cov.get("unclassifiedPct"));
                m.put("unclassifiedAmount", cov.get("unclassifiedAmount"));
            }
        } catch (Exception ignored) {
            // view/table may be unavailable in test env
        }
    }

    private int safeOrphanCount(String owner) {
        try {
            return dataQualityMapper.countOrphanCategoryTransactions(owner);
        } catch (Exception e) {
            return 0;
        }
    }

    private int safeRefundCount(String owner) {
        try {
            return dataQualityMapper.countRefundExcluded(owner);
        } catch (Exception e) {
            return 0;
        }
    }

    private void putMerchantCoverage(Map<String, Object> m, String owner) {
        try {
            Map<String, Object> mc = dataQualityMapper.merchantTokenCoverage(owner);
            if (mc != null) {
                m.put("merchantTokenCoveragePct", mc.get("tokenCoveragePct"));
            }
        } catch (Exception ignored) {
            // optional
        }
    }

    private static String confidenceLevel(Map<String, Object> strip) {
        Object pctObj = strip.get("unclassifiedPct");
        double pct = pctObj instanceof Number n ? n.doubleValue() : 100;
        if (pct >= 15) {
            return "low";
        }
        if (pct >= 5) {
            return "medium";
        }
        return "high";
    }
}
