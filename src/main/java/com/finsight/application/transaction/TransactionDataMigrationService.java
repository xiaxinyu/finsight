package com.finsight.application.transaction;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TransactionDataMigrationService {

    private static final Logger log = LoggerFactory.getLogger(TransactionDataMigrationService.class);

    private final TransactionRepository transactionRepository;
    private final AuthenticationFacade authenticationFacade;
    private final ITransactionService transactionService;

    public TransactionDataMigrationService(TransactionRepository transactionRepository,
                                           AuthenticationFacade authenticationFacade,
                                           ITransactionService transactionService) {
        this.transactionRepository = transactionRepository;
        this.authenticationFacade = authenticationFacade;
        this.transactionService = transactionService;
    }

    /**
     * Idempotent: normalizes legacy signed / dual-field transaction rows.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> normalizeTransactionAmounts() {
        List<String> ids = transactionRepository.listIdsNeedingAmountNormalization();
        int scanned = ids == null ? 0 : ids.size();
        int corrected = 0;
        String userName = authenticationFacade.getUserName();
        if (ids != null) {
            for (String id : ids) {
                Transaction existing = transactionRepository.selectById(id);
                if (existing == null) {
                    continue;
                }
                Transaction before = snapshot(existing);
                TransactionAmountNormalizer.normalize(existing);
                if (!amountsEqual(before, existing)) {
                    existing.setUpdateUser(userName);
                    transactionRepository.updateTransaction(existing);
                    corrected++;
                }
            }
        }
        if (corrected > 0) {
            transactionService.invalidateHomeSummaryCache();
        }
        log.info("normalizeTransactionAmounts: scanned={}, corrected={}", scanned, corrected);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scanned", scanned);
        result.put("corrected", corrected);
        return result;
    }

    private static Transaction snapshot(Transaction t) {
        Transaction copy = new Transaction();
        copy.setIncomeMoney(t.getIncomeMoney());
        copy.setBalanceMoney(t.getBalanceMoney());
        copy.setTxnKind(t.getTxnKind());
        return copy;
    }

    private static boolean amountsEqual(Transaction a, Transaction b) {
        return Objects.equals(a.getIncomeMoney(), b.getIncomeMoney())
                && Objects.equals(a.getBalanceMoney(), b.getBalanceMoney())
                && Objects.equals(a.getTxnKind(), b.getTxnKind());
    }
}
