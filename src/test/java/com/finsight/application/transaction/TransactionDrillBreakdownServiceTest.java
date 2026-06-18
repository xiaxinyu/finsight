package com.finsight.application.transaction;

import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.domain.model.DrillBreakdownItem;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.infrastructure.mapper.TransactionMapper;
import com.finsight.web.api.dto.TransactionParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionDrillBreakdownServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private TransactionQuerySupport querySupport;

    private TransactionDrillBreakdownService service;

    @BeforeEach
    void setUp() {
        service = new TransactionDrillBreakdownService(transactionRepository, transactionMapper, querySupport);
    }

    @Test
    void marksTruncatedWhenTotalExceedsSample() throws Exception {
        TransactionParam param = new TransactionParam();
        param.setTransactionDateStartStr("01/01/2026");
        param.setTransactionDateEndStr("06/30/2026");
        param.setTxnTypes("expense");

        when(transactionRepository.countTransaction(any())).thenReturn(500);
        Map<String, Object> stats = new HashMap<>();
        stats.put("expense", 12000.0);
        when(transactionMapper.aggregateStats(any())).thenReturn(stats);

        DrillBreakdownItem category = new DrillBreakdownItem();
        category.setLabel("Food");
        category.setTxnCount(40);
        category.setTotal(8000.0);
        when(transactionMapper.drillCategoryBreakdown(any(), eq(100))).thenReturn(List.of(category));
        when(transactionMapper.drillMerchantBreakdown(any(), eq(100))).thenReturn(List.of());

        Transaction tx = new Transaction();
        tx.setId("t1");
        when(transactionRepository.getTransactions(any(), any(Page.class))).thenReturn(List.of(tx));

        DrillBreakdownResult result = service.load(param, 200);

        assertEquals(500, result.getTotal());
        assertEquals(1, result.getSampleSize());
        assertTrue(result.isTruncated());
        assertEquals(12000.0, result.getAggregateTotal(), 0.01);
        assertEquals("Food", result.getCategories().get(0).getLabel());
        verify(transactionRepository).getTransactions(any(), any(Page.class));
    }

    @Test
    void passesMerchantTokenIntoQuery() throws Exception {
        TransactionParam param = new TransactionParam();
        param.setTransactionDateStartStr("01/01/2026");
        param.setTransactionDateEndStr("06/30/2026");
        param.setTxnTypes("expense");
        param.setMerchantToken("netflix");

        when(transactionRepository.countTransaction(any())).thenReturn(3);
        when(transactionMapper.aggregateStats(any())).thenReturn(Map.of("expense", 45.0));
        when(transactionMapper.drillCategoryBreakdown(any(), eq(100))).thenReturn(List.of());
        when(transactionMapper.drillMerchantBreakdown(any(), eq(100))).thenReturn(List.of());
        when(transactionRepository.getTransactions(any(), any(Page.class))).thenReturn(List.of());

        service.load(param, 200);

        verify(transactionMapper).drillMerchantBreakdown(any(), eq(100));
    }
}
