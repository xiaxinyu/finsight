package com.finsight.application.classification;

import com.finsight.application.card.BankCardService;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.application.transaction.TransactionReclassificationService;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.web.api.dto.RuleImpactPreviewDto;
import com.finsight.web.api.dto.RuleImpactPreviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleImpactPreviewServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ClassificationService classificationService;
    @Mock
    private ConsumeCategoryService categoryService;
    @Mock
    private RulePatternMatcher patternMatcher;
    @Mock
    private BankCardService bankCardService;

    private RuleImpactPreviewService service;

    @BeforeEach
    void setUp() {
        service = new RuleImpactPreviewService(
                transactionRepository, classificationService, categoryService, patternMatcher, bankCardService);
    }

    @Test
    void preview_unclassifiedOnly_skipsClassifiedMatches() {
        Transaction unclassified = txn("t1", null, null, "美团外卖", 28.0);
        Transaction classified = txn("t2", "FOOD", "Food", "京东", 50.0);

        when(transactionRepository.getTransactions(any(), any(Page.class)))
                .thenReturn(List.of(unclassified, classified));
        when(patternMatcher.matchesTransaction(any(), any(), any(), any())).thenReturn(true);

        ConsumeCategory target = new ConsumeCategory();
        target.setCode("LIVING-01");
        target.setName("Food delivery");
        lenient().when(categoryService.listAll()).thenReturn(List.of(target));

        RuleImpactPreviewRequest req = new RuleImpactPreviewRequest();
        req.setPattern("美团");
        req.setPatternType("contains");
        req.setCategoryId("LIVING-01");
        req.setScope("UNCLASSIFIED_ONLY");

        RuleImpactPreviewDto out = service.preview(req);

        assertEquals(1, out.getMatchedCount());
        assertEquals(1, out.getUnclassifiedMatchCount());
        assertEquals(0, out.getWouldOverrideCount());
    }

    private static Transaction txn(String id, String code, String name, String desc, double amount) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setConsumeCode(code);
        tx.setConsumeName(name);
        tx.setCategoryCode(code);
        tx.setCategoryName(name);
        tx.setTransactionDesc(desc);
        tx.setExpenseAmount(amount);
        tx.setTransactionDate(new Date());
        return tx;
    }
}
