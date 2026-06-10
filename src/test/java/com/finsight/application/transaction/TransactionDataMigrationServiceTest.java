package com.finsight.application.transaction;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionDataMigrationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AuthenticationFacade authenticationFacade;
    @Mock
    private ITransactionService transactionService;

    @InjectMocks
    private TransactionDataMigrationService migrationService;

    @Test
    void normalize_correctsNegativeBalanceRow() {
        when(transactionRepository.listIdsNeedingAmountNormalization()).thenReturn(List.of("t1"));
        when(authenticationFacade.getUserName()).thenReturn("admin");
        Transaction row = new Transaction();
        row.setId("t1");
        row.setBalanceMoney(-50.0);
        when(transactionRepository.selectById("t1")).thenReturn(row);

        Map<String, Object> result = migrationService.normalizeTransactionAmounts();

        assertEquals(1, result.get("scanned"));
        assertEquals(1, result.get("corrected"));
        verify(transactionRepository).updateTransaction(any(Transaction.class));
        verify(transactionService).invalidateHomeSummaryCache();
    }

    @Test
    void normalize_skipsAlreadyCanonicalRow() {
        when(transactionRepository.listIdsNeedingAmountNormalization()).thenReturn(List.of("t2"));
        when(authenticationFacade.getUserName()).thenReturn("admin");
        Transaction row = new Transaction();
        row.setId("t2");
        row.setIncomeMoney(100.0);
        row.setBalanceMoney(100.0);
        when(transactionRepository.selectById("t2")).thenReturn(row);

        Map<String, Object> result = migrationService.normalizeTransactionAmounts();

        assertEquals(1, result.get("scanned"));
        assertEquals(1, result.get("corrected"));
        verify(transactionService).invalidateHomeSummaryCache();
    }

    @Test
    void normalize_emptyList() {
        when(transactionRepository.listIdsNeedingAmountNormalization()).thenReturn(List.of());

        Map<String, Object> result = migrationService.normalizeTransactionAmounts();

        assertEquals(0, result.get("scanned"));
        assertEquals(0, result.get("corrected"));
        verify(transactionRepository, never()).updateTransaction(any());
        verify(transactionService, never()).invalidateHomeSummaryCache();
    }
}
