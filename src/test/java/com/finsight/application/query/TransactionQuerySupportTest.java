package com.finsight.application.query;

import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.port.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionQuerySupportTest {

    private CategoryRepository categoryRepository;
    private TransactionQuerySupport support;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        support = new TransactionQuerySupport(categoryRepository);
        ConsumeCategory parent = category("FOOD", "FOOD", "Food");
        ConsumeCategory child = category("FOOD-01", "FOOD-01", "Dining out");
        child.setParentId("FOOD");
        when(categoryRepository.listActive()).thenReturn(List.of(parent, child));
    }

    @Test
    void expandCategoryFilter_includesParentAndChildren() {
        TransactionQuery q = new TransactionQuery();
        q.setConsumes(new String[] {"FOOD"});
        support.expandCategoryFilter(q);
        List<String> expanded = List.of(q.getConsumes());
        assertTrue(expanded.contains("FOOD"));
        assertTrue(expanded.contains("FOOD-01"));
    }

    private static ConsumeCategory category(String id, String code, String name) {
        ConsumeCategory c = new ConsumeCategory();
        c.setId(id);
        c.setCode(code);
        c.setName(name);
        c.setDeleted(0);
        return c;
    }
}
