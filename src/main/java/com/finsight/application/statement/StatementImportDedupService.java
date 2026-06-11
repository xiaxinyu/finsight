package com.finsight.application.statement;

import com.finsight.infrastructure.mapper.FinancialMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Skips staging rows on commit when the ledger already has a matching transaction.
 * Ledger-wide duplicate scanning is not performed; use {@code scripts/db/*duplicate*.sql} for cleanup.
 */
@Service
public class StatementImportDedupService {

    private final FinancialMapper financialMapper;

    public StatementImportDedupService(FinancialMapper financialMapper) {
        this.financialMapper = financialMapper;
    }

    public List<String> findAlreadyImportedTempIds(String statementId) {
        if (statementId == null || statementId.isBlank()) {
            return Collections.emptyList();
        }
        List<String> ids = financialMapper.findDuplicatePreviewTempIds(statementId);
        return ids == null ? Collections.emptyList() : ids;
    }
}
