package com.finsight.application.transaction.impl;

import com.finsight.application.analytics.MetricRefreshTrigger;
import com.finsight.application.card.BankCardService;
import com.finsight.application.transaction.TransactionCategoryFieldNormalizer;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplKindSwitchTest {

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
    void expenseToIncome_batchPreservesAmount() throws Exception {
        Transaction tx = new Transaction();
        tx.setId("tx-1");
        tx.setBalanceMoney(1500.0);
        tx.setIncomeMoney(0.0);
        tx.setTxnKind("expense");
        when(transactionRepository.selectById("tx-1")).thenReturn(tx);

        int count = service.expenseToIncome(List.of("tx-1"), "user1");

        assertEquals(1, count);
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).updateTransaction(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals(1500.0, saved.getIncomeMoney());
        assertEquals(0.0, saved.getBalanceMoney());
        assertEquals("income", saved.getTxnKind());
        verify(metricRefreshTrigger).afterTransactionsChanged(any(), eq("user1"));
    }

    @Test
    void incomeToExpense_batchPreservesAmount() throws Exception {
        Transaction tx = new Transaction();
        tx.setId("tx-2");
        tx.setIncomeMoney(750.0);
        tx.setBalanceMoney(0.0);
        tx.setTxnKind("income");
        when(transactionRepository.selectById("tx-2")).thenReturn(tx);

        int count = service.incomeToExpense(List.of("tx-2"), "user1");

        assertEquals(1, count);
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).updateTransaction(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals(750.0, saved.getBalanceMoney());
        assertEquals(0.0, saved.getIncomeMoney());
        assertEquals("expense", saved.getTxnKind());
    }
}
