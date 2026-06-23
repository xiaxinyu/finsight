package com.finsight.application.finance;

import com.finsight.application.classification.ConfigVersionService;
import com.finsight.infrastructure.mapper.DataQualityMapper;
import com.finsight.infrastructure.mapper.FinancialMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DataQualityService {

    private final FinancialMapper financialMapper;
    private final DataQualityMapper dataQualityMapper;
    private final ConfigVersionService configVersionService;

    public DataQualityService(FinancialMapper financialMapper,
                              DataQualityMapper dataQualityMapper,
                              ConfigVersionService configVersionService) {
        this.financialMapper = financialMapper;
        this.dataQualityMapper = dataQualityMapper;
        this.configVersionService = configVersionService;
    }

    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("unclassifiedCount", financialMapper.countUnclassified());
        m.put("transferPairCount", financialMapper.countTransferGroups());
        putCoverage(m);
        m.put("orphanCategoryTxnCount", safeOrphanCount());
        m.put("refundExcludedCount", safeRefundCount());
        putMerchantCoverage(m);
        m.put("versions", configVersionService.asMap());
        return m;
    }

    public Map<String, Object> reportStrip(String metricsSource) {
        Map<String, Object> strip = summary();
        strip.put("metricsSource", metricsSource == null ? "fin_metric_monthly" : metricsSource);
        strip.put("confidence", confidenceLevel(strip));
        return strip;
    }

    private void putCoverage(Map<String, Object> m) {
        try {
            Map<String, Object> cov = dataQualityMapper.classificationCoverage();
            if (cov != null) {
                m.put("totalTxnCount", cov.get("totalTxns"));
                m.put("unclassifiedPct", cov.get("unclassifiedPct"));
                m.put("unclassifiedAmount", cov.get("unclassifiedAmount"));
            }
        } catch (Exception ignored) {
            // view/table may be unavailable in test env
        }
    }

    private int safeOrphanCount() {
        try {
            return dataQualityMapper.countOrphanCategoryTransactions();
        } catch (Exception e) {
            return 0;
        }
    }

    private int safeRefundCount() {
        try {
            return dataQualityMapper.countRefundExcluded();
        } catch (Exception e) {
            return 0;
        }
    }

    private void putMerchantCoverage(Map<String, Object> m) {
        try {
            Map<String, Object> mc = dataQualityMapper.merchantTokenCoverage();
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
