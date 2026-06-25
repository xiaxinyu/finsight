package com.finsight.application.transaction.impl;

import com.finsight.application.analytics.MetricRefreshTrigger;
import com.finsight.application.card.BankCardService;
import com.finsight.application.transaction.TransactionCategoryFieldNormalizer;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplDeleteTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BankCardService bankCardService;
    @Mock
    private MetricRefreshTrigger metricRefreshTrigger;
    @Mock
    private TransactionCategoryFieldNormalizer categoryFieldNormalizer;

    private TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "transactionRepository", transactionRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "bankCardService", bankCardService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "metricRefreshTrigger", metricRefreshTrigger);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "categoryFieldNormalizer", categoryFieldNormalizer);
    }

    @Test
    void deleteTransaction_softDeletesActiveRow() throws Exception {
        Transaction tx = new Transaction();
        tx.setId("tx-1");
        tx.setDeleted(0);
        when(transactionRepository.selectById("tx-1")).thenReturn(tx);

        service.deleteTransaction("tx-1", "user1");

        verify(transactionRepository).deleteTransaction("tx-1", "user1");
        verify(metricRefreshTrigger).afterTransactionsChanged(any(), eq("user1"));
    }

    @Test
    void deleteTransaction_rejectsAlreadyDeleted() {
        Transaction tx = new Transaction();
        tx.setId("tx-1");
        tx.setDeleted(1);
        when(transactionRepository.selectById("tx-1")).thenReturn(tx);

        assertThrows(com.finsight.common.exception.AppServiceException.class,
                () -> service.deleteTransaction("tx-1", "user1"));
        verify(transactionRepository, never()).deleteTransaction(any(), any());
    }
}
