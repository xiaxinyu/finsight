package com.finsight.application.statement;

import com.finsight.application.authentication.LedgerUserScope;
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
    private final LedgerUserScope ledgerUserScope;

    public StatementImportDedupService(FinancialMapper financialMapper, LedgerUserScope ledgerUserScope) {
        this.financialMapper = financialMapper;
        this.ledgerUserScope = ledgerUserScope;
    }

    public List<String> findAlreadyImportedTempIds(String statementId) {
        if (statementId == null || statementId.isBlank()) {
            return Collections.emptyList();
        }
        List<String> ids = financialMapper.findDuplicatePreviewTempIds(statementId, ledgerUserScope.resolve());
        return ids == null ? Collections.emptyList() : ids;
    }
}
