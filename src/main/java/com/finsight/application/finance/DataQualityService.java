package com.finsight.application.finance;

import com.finsight.infrastructure.mapper.FinancialMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
        m.put("duplicateGroupCount", financialMapper.countDuplicateFingerprints());
        m.put("duplicateExcessCount", financialMapper.countDuplicateExcessRows());
        m.put("duplicateCount", financialMapper.countDuplicateExcessRows());
        m.put("transferPairCount", financialMapper.countTransferGroups());
        return m;
    }

    public List<String> duplicatePreviewTempIds(String statementId) {
        if (statementId == null || statementId.isBlank()) {
            return Collections.emptyList();
        }
        List<String> ids = financialMapper.findDuplicatePreviewTempIds(statementId);
        return ids == null ? Collections.emptyList() : ids;
    }
}
