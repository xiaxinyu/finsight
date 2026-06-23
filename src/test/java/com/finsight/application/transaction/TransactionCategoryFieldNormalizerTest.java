package com.finsight.application.transaction;

import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionCategoryFieldNormalizerTest {

    @Mock
    private ConsumeCategoryService categoryService;

    private TransactionCategoryFieldNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new TransactionCategoryFieldNormalizer(categoryService);
    }

    @Test
    void normalizeDerivesNameFromActiveCategory() {
        ConsumeCategory cat = new ConsumeCategory();
        cat.setCode("DAILY-01");
        cat.setName("Daily food");
        cat.setDeleted(0);
        when(categoryService.getOne(any(), any(Boolean.class))).thenReturn(cat);

        Transaction t = new Transaction();
        t.setConsumeCode("DAILY-01");

        normalizer.normalize(t);

        assertEquals("DAILY-01", t.getConsumeCode());
        assertEquals("DAILY-01", t.getConsumeID());
        assertEquals("Daily food", t.getConsumeName());
        assertEquals("Daily food", t.getCategoryName());
    }
}
