package com.finsight.domain.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MetricMonthlyRepository {

    void upsert(String userId, String yearMonth, String metricCode, BigDecimal value);

    BigDecimal find(String userId, String yearMonth, String metricCode);

    List<Map<String, Object>> listForUser(String userId, String fromYm, String toYm);
}
