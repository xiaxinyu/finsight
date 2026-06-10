package com.finsight.application.finance;

import com.finsight.domain.model.Bill;
import com.finsight.infrastructure.mapper.FinancialMapper;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CashflowService {

    private final FinancialMapper financialMapper;
    private final FinancialAccountService accountService;
    private final BillService billService;

    public CashflowService(FinancialMapper financialMapper,
                           FinancialAccountService accountService,
                           BillService billService) {
        this.financialMapper = financialMapper;
        this.accountService = accountService;
        this.billService = billService;
    }

    public Map<String, Object> metrics() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date since30 = cal.getTime();

        double expense30 = safe(financialMapper.sumExpenseSince(since30));
        double burnRate = expense30 / 30.0;

        double liquid = accountService.latestBalances().stream()
                .mapToDouble(kv -> parseAmount(kv.getValue()))
                .sum();

        double billsReserved = sumBillsNext30Days();
        double safeToSpend = liquid - billsReserved;
        double runwayMonths = burnRate > 0 ? liquid / (burnRate * 30) : 0;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("safeToSpend", safeToSpend);
        m.put("burnRateDaily", burnRate);
        m.put("runwayMonths", runwayMonths);
        m.put("liquidAssets", liquid);
        m.put("billsReserved", billsReserved);
        return m;
    }

    private double sumBillsNext30Days() {
        List<Bill> bills = billService.listEnabled();
        if (bills.isEmpty()) {
            return 0;
        }
        Calendar cal = Calendar.getInstance();
        double total = 0;
        for (int i = 0; i < 30; i++) {
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            for (Bill b : bills) {
                if (b.getDueDay() != null && b.getDueDay() == dayOfMonth && b.getAmount() != null) {
                    total += b.getAmount().doubleValue();
                }
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return total;
    }

    private static double safe(Double v) {
        return v == null ? 0 : v;
    }

    private static double parseAmount(String v) {
        if (v == null || v.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
