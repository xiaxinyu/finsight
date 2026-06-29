package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticBreakdownServiceTest {

    @Mock
    private SemanticBreakdownRepository breakdownRepository;
    @Mock
    private AuthenticationFacade authenticationFacade;

    @InjectMocks
    private SemanticBreakdownService semanticBreakdownService;

    @Test
    void expenseBreakdown_groupsFixedAndVariableShares() {
        when(authenticationFacade.getUserName()).thenReturn("alice");
        when(breakdownRepository.expenseBySemanticTag(any(), any(), any())).thenReturn(List.of(
                new SemanticBreakdownRepository.TagAmountRow("dining_spending", 3000),
                new SemanticBreakdownRepository.TagAmountRow("fixed_housing", 5000),
                new SemanticBreakdownRepository.TagAmountRow("medical_spending", 2000)));

        Map<String, Object> out = semanticBreakdownService.expenseBreakdown("01/01/2026", "01/31/2026");

        assertEquals(10000.0, out.get("expenseTotal"));
        assertEquals(5000.0, out.get("fixedTotal"));
        assertEquals(5000.0, out.get("variableTotal"));
        assertEquals(50.0, out.get("fixedSharePct"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) out.get("rows");
        assertEquals("Dining", rows.get(0).get("label"));
        assertEquals("expense", rows.get(0).get("group"));
        assertEquals("Housing", rows.get(1).get("label"));
        assertEquals("fixed", rows.get(1).get("group"));
    }
}
