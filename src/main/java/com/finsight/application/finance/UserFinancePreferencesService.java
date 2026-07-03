package com.finsight.application.finance;

import com.finsight.common.exception.AppServiceException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserFinancePreferencesService {

    private static final int[] DEFAULT_INCOME_PAY_DAYS = {5, 20};

    private final PlanningPreferencesStore planningStore;

    public UserFinancePreferencesService(PlanningPreferencesStore planningStore) {
        this.planningStore = planningStore;
    }

    public int[] incomePayDays() {
        return planningStore.getIncomePayDays();
    }

    public List<Integer> incomePayDaysList() {
        return Arrays.stream(incomePayDays()).boxed().collect(Collectors.toList());
    }

    public List<Integer> updateIncomePayDays(List<Integer> days) throws AppServiceException {
        return planningStore.setIncomePayDays(days);
    }
}
