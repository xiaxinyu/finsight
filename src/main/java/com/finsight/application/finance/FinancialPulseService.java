package com.finsight.application.finance;

import com.finsight.domain.model.KeyValue;
import com.finsight.infrastructure.mapper.FinancialMapper;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinancialPulseService {

    private final FinancialAccountService accountService;
    private final FinancialMapper financialMapper;
    private final DataQualityService dataQualityService;

    public FinancialPulseService(FinancialAccountService accountService,
                                 FinancialMapper financialMapper,
                                 DataQualityService dataQualityService) {
        this.accountService = accountService;
        this.financialMapper = financialMapper;
        this.dataQualityService = dataQualityService;
    }

    public Map<String, Object> pulse() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date monthStart = cal.getTime();

        double incomeMtd = safe(financialMapper.sumIncomeSince(monthStart));
        double expenseMtd = safe(financialMapper.sumExpenseSince(monthStart));
        double fixedYear = safe(financialMapper.sumFixedBucketYear(year));
        Calendar ytd = Calendar.getInstance();
        ytd.set(Calendar.MONTH, 0);
        ytd.set(Calendar.DAY_OF_MONTH, 1);
        double incomeYtd = safe(financialMapper.sumIncomeSince(ytd.getTime()));

        List<KeyValue> accounts = accountService.latestBalances();
        double liquidAssets = accounts.stream()
                .mapToDouble(kv -> parseAmount(kv.getValue()))
                .sum();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accounts", accounts);
        out.put("incomeMtd", incomeMtd);
        out.put("expenseMtd", expenseMtd);
        out.put("netFlowMtd", incomeMtd - expenseMtd);
        out.put("fixedExpenseYear", fixedYear);
        out.put("fixedExpenseRatio", incomeYtd > 0 ? fixedYear / incomeYtd : 0);
        out.put("incomeYtd", incomeYtd);
        out.put("liquidAssets", liquidAssets);
        out.put("dataQuality", dataQualityService.summary());
        return out;
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
