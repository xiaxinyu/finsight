package com.finsight.application.finance;

import com.finsight.application.authentication.AuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanningPreferencesStorePayDaysTest {

    @Mock
    private AuthenticationFacade authenticationFacade;

    private PlanningPreferencesStore store;

    @BeforeEach
    void setUp() {
        store = new PlanningPreferencesStore(authenticationFacade);
    }

    @Test
    void defaultIncomePayDays_areFifthAndTwentieth() {
        when(authenticationFacade.getUserName()).thenReturn("alice");
        assertArrayEquals(new int[] {5, 20}, store.getIncomePayDays());
    }

    @Test
    void setIncomePayDays_normalizesSortsAndDedupes() {
        when(authenticationFacade.getUserName()).thenReturn("alice");
        List<Integer> saved = store.setIncomePayDays(List.of(20, 5, 20, 15));
        assertEquals(List.of(5, 15, 20), saved);
        assertArrayEquals(new int[] {5, 15, 20}, store.getIncomePayDays());
    }

    @Test
    void setIncomePayDays_rejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> store.setIncomePayDays(List.of(0)));
        assertThrows(IllegalArgumentException.class, () -> store.setIncomePayDays(List.of(29)));
    }
}
