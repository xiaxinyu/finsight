package com.finsight.application.finance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFinancePreferencesServiceTest {

    @Mock
    private PlanningPreferencesStore planningStore;

    @InjectMocks
    private UserFinancePreferencesService service;

    @Test
    void incomePayDays_returnsStoreValue() {
        when(planningStore.getIncomePayDays()).thenReturn(new int[] {5, 15, 25});
        assertEquals(List.of(5, 15, 25), service.incomePayDaysList());
    }

    @Test
    void updateIncomePayDays_delegatesToStore() throws Exception {
        when(planningStore.setIncomePayDays(List.of(10, 25))).thenReturn(List.of(10, 25));
        assertEquals(List.of(10, 25), service.updateIncomePayDays(List.of(10, 25)));
    }

    @Test
    void incomePayDaysList_emptyWhenStoreEmpty() {
        when(planningStore.getIncomePayDays()).thenReturn(new int[] {});
        assertEquals(List.of(), service.incomePayDaysList());
    }
}
