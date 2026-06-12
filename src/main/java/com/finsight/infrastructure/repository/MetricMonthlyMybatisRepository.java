package com.finsight.infrastructure.repository;

import com.finsight.domain.port.MetricMonthlyRepository;
import com.finsight.infrastructure.mapper.FinPlanningMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public class MetricMonthlyMybatisRepository implements MetricMonthlyRepository {

    private final FinPlanningMapper mapper;

    public MetricMonthlyMybatisRepository(FinPlanningMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void upsert(String userId, String yearMonth, String metricCode, BigDecimal value) {
        mapper.upsertMetric(userId, yearMonth, metricCode, value);
    }

    @Override
    public BigDecimal find(String userId, String yearMonth, String metricCode) {
        return mapper.findMetric(userId, yearMonth, metricCode);
    }

    @Override
    public List<Map<String, Object>> listForUser(String userId, String fromYm, String toYm) {
        return mapper.listMetrics(userId, fromYm, toYm);
    }
}
