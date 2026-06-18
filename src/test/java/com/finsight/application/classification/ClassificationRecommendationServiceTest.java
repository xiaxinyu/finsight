package com.finsight.application.classification;

import com.finsight.application.consume.ClassificationService;
import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassificationRecommendationServiceTest {

    @Mock
    private ClassificationService classificationService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ConsumeCategoryService categoryService;

    private ClassificationRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new ClassificationRecommendationService(classificationService, transactionRepository, categoryService);
    }

    @Test
    void recommendsHeuristicForInstallmentDescription() {
        when(classificationService.suggestRelaxed(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(transactionRepository.getTransactions(any(), any(Page.class))).thenReturn(List.of());

        ConsumeCategory shopping = new ConsumeCategory();
        shopping.setCode("SHOP-01");
        shopping.setName("网购购物");
        when(categoryService.listAll()).thenReturn(List.of(shopping));

        Transaction tx = new Transaction();
        tx.setId("1");
        tx.setTransactionDesc("(分期) 邮购分期24029247887/09/12期");

        Optional<CategoryRecommendation> rec = service.recommend(tx, "CMB", "CREDIT");
        assertTrue(rec.isPresent());
        assertEquals(CategoryRecommendation.Source.HEURISTIC, rec.get().getSource());
        assertEquals("SHOP-01", rec.get().getCategoryCode());
    }

    @Test
    void fallsBackToKeywordsWhenNoCategoryFound() {
        when(classificationService.suggestRelaxed(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(transactionRepository.getTransactions(any(), any(Page.class))).thenReturn(List.of());
        when(categoryService.listAll()).thenReturn(List.of());
        when(classificationService.tokens(any()))
                .thenReturn(List.of("邮购分期", "分期"));

        Transaction tx = new Transaction();
        tx.setId("2");
        tx.setTransactionDesc("(分期) 邮购分期24029247887/09/12期");

        Optional<CategoryRecommendation> rec = service.recommend(tx, "", "");
        assertTrue(rec.isPresent());
        assertEquals(CategoryRecommendation.Source.KEYWORDS, rec.get().getSource());
        assertTrue(rec.get().getSuggestedKeywords().contains("邮购分期"));
    }
}
