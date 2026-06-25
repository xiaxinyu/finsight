package com.finsight.application.transaction;

import com.finsight.application.analytics.MetricRefreshTrigger;
import com.finsight.application.card.BankCardService;
import com.finsight.application.classification.ClassificationRecommendationService;
import com.finsight.application.consume.ClassificationService;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.web.api.dto.ReclassificationAssignmentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionReclassificationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ClassificationService classificationService;
    @Mock
    private BankCardService bankCardService;
    @Mock
    private ITransactionService transactionService;
    @Mock
    private ClassificationRecommendationService recommendationService;
    @Mock
    private MetricRefreshTrigger metricRefreshTrigger;

    private TransactionReclassificationService service;

    @BeforeEach
    void setUp() {
        service = new TransactionReclassificationService(
                transactionRepository,
                classificationService,
                bankCardService,
                transactionService,
                recommendationService,
                metricRefreshTrigger);
    }

    @Test
    void applyAssignments_persistsUserConfirmedCategory() throws Exception {
        Transaction tx = new Transaction();
        tx.setId("tx-1");
        tx.setTransactionDesc("Test meal");
        when(transactionRepository.selectById("tx-1")).thenReturn(tx);

        ReclassificationAssignmentDto assignment = new ReclassificationAssignmentDto();
        assignment.setTransactionId("tx-1");
        assignment.setCategoryCode("DAILY-01");
        assignment.setCategoryName("Daily food");

        TransactionReclassificationResult result = service.applyAssignments(
                java.util.List.of(assignment), "test-user");

        assertEquals(1, result.getClassified());
        verify(transactionService).updateTransaction(any(Transaction.class), eq("test-user"));
        verify(metricRefreshTrigger).afterTransactionsChanged(any(), eq("test-user"));
    }
}
