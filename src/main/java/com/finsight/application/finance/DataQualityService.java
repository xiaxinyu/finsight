package com.finsight.application.finance;

import com.finsight.infrastructure.mapper.FinancialMapper;
import com.finsight.infrastructure.mapper.TransferPairMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.domain.model.TransferPair;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataQualityService {

    private final FinancialMapper financialMapper;
    private final TransferPairMapper transferPairMapper;

    public DataQualityService(FinancialMapper financialMapper, TransferPairMapper transferPairMapper) {
        this.financialMapper = financialMapper;
        this.transferPairMapper = transferPairMapper;
    }

    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("unclassifiedCount", financialMapper.countUnclassified());
        m.put("duplicateCount", financialMapper.countDuplicateFingerprints());
        Long transfers = transferPairMapper.selectCount(Wrappers.<TransferPair>lambdaQuery().eq(TransferPair::getDeleted, 0));
        m.put("transferPairCount", transfers == null ? 0 : transfers);
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
