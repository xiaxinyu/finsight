package com.finsight.application.statement;

import com.finsight.application.consume.ClassificationProperties;
import com.finsight.application.consume.ClassificationService;
import com.finsight.domain.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementImportClassifierTest {

    @Mock
    private ClassificationService classificationService;

    @Mock
    private ClassificationProperties classificationProperties;

    @Mock
    private ImportCategoryHeuristic importHeuristic;

    @InjectMocks
    private StatementImportClassifier classifier;

    private Transaction tx;

    @BeforeEach
    void setUp() {
        tx = new Transaction();
        tx.setTransactionDesc("财付通-深圳市地铁相关运营主体");
        tx.setBalanceMoney(6.0);
    }

    @Test
    void overridesWrongInvestRuleWithTransitHeuristic() {
        ClassificationService.Result wrong = new ClassificationService.Result();
        wrong.id = "INVEST-01";
        wrong.name = "基金申购（买入基金）";

        ImportCategoryHeuristic.Match transit = new ImportCategoryHeuristic.Match(
                ImportCategoryHeuristic.Family.TRANSIT,
                "TRAVEL-01",
                "公共交通",
                "Merchant pattern");

        when(classificationService.classify(any(), eq("CCB"), eq("credit"), eq(6.0), any()))
                .thenReturn(wrong);
        when(importHeuristic.match(eq("深圳市地铁相关运营主体"), eq(6.0)))
                .thenReturn(Optional.of(transit));
        when(importHeuristic.shouldOverrideRule("INVEST-01", "基金申购（买入基金）", transit,
                "深圳市地铁相关运营主体"))
                .thenReturn(true);

        classifier.classify(tx, "CCB", "credit");

        assertEquals("TRAVEL-01", tx.getCategoryCode());
        assertEquals("TRAVEL-01", tx.getConsumeCode());
        assertEquals("TRAVEL-01", tx.getConsumeID());
        assertEquals("公共交通", tx.getCategoryName());
        verify(importHeuristic).shouldOverrideRule("INVEST-01", "基金申购（买入基金）", transit,
                "深圳市地铁相关运营主体");
    }
}
