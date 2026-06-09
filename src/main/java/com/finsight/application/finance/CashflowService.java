package com.finsight.application.finance;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.domain.model.Bill;
import com.finsight.infrastructure.mapper.BillMapper;
import com.finsight.infrastructure.mapper.FinancialMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CashflowService {

    private final FinancialMapper financialMapper;
    private final FinancialAccountService accountService;
    private final BillMapper billMapper;

    public CashflowService(FinancialMapper financialMapper,
                           FinancialAccountService accountService,
                           BillMapper billMapper) {
        this.financialMapper = financialMapper;
        this.accountService = accountService;
        this.billMapper = billMapper;
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

        double billsReserved = sumEnabledBills();
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

    private double sumEnabledBills() {
        List<Bill> bills = billMapper.selectList(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getEnabled, 1).eq(Bill::getDeleted, 0));
        return bills.stream()
                .map(Bill::getAmount)
                .map(a -> a == null ? BigDecimal.ZERO : a)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
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
