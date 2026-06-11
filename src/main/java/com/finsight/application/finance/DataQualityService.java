package com.finsight.application.finance;

import com.finsight.infrastructure.mapper.FinancialMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DataQualityService {

    private final FinancialMapper financialMapper;

    public DataQualityService(FinancialMapper financialMapper) {
        this.financialMapper = financialMapper;
    }

    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("unclassifiedCount", financialMapper.countUnclassified());
        m.put("transferPairCount", financialMapper.countTransferGroups());
        return m;
    }
}
