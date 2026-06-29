package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.classification.FinanceSemanticsCatalog;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SemanticBreakdownService {

    private static final DateTimeFormatter US = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final SemanticBreakdownRepository breakdownRepository;
    private final AuthenticationFacade authenticationFacade;

    public SemanticBreakdownService(SemanticBreakdownRepository breakdownRepository,
            AuthenticationFacade authenticationFacade) {
        this.breakdownRepository = breakdownRepository;
        this.authenticationFacade = authenticationFacade;
    }

    public Map<String, Object> expenseBreakdown(String fromStr, String toStr, String cardId, String consumeId) {
        LocalDate end = parseEnd(toStr);
        LocalDate start = parseStart(fromStr, end);

        List<SemanticBreakdownRepository.TagAmountRow> raw = breakdownRepository.expenseBySemanticTag(
                new SemanticBreakdownRepository.SemanticBreakdownQuery(
                        userKey(), start, end, cardId, consumeId));

        double expenseTotal = raw.stream().mapToDouble(SemanticBreakdownRepository.TagAmountRow::amount).sum();
        double fixedTotal = 0;
        double variableTotal = 0;

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SemanticBreakdownRepository.TagAmountRow row : raw) {
            String group = FinanceSemanticsCatalog.semanticTagGroup(row.tagId());
            if ("fixed".equals(group)) {
                fixedTotal += row.amount();
            } else if ("expense".equals(group) || "other".equals(group)) {
                variableTotal += row.amount();
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("tagId", row.tagId());
            String classL1 = FinanceSemanticsCatalog.semanticTagPickerRowLabel(row.tagId());
            String classL2 = FinanceSemanticsCatalog.semanticTagLevel2Label(row.tagId());
            out.put("classL1", classL1);
            out.put("classL2", classL2);
            out.put("classification", FinanceSemanticsCatalog.semanticTagClassification(row.tagId()));
            out.put("txnType", FinanceSemanticsCatalog.semanticTagTxnTypeLabel(row.tagId()));
            out.put("label", classL2);
            out.put("group", group);
            out.put("amount", round2(row.amount()));
            out.put("sharePct", expenseTotal > 0 ? round1(row.amount() * 100.0 / expenseTotal) : 0.0);
            rows.add(out);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rows", rows);
        payload.put("expenseTotal", round2(expenseTotal));
        payload.put("fixedTotal", round2(fixedTotal));
        payload.put("variableTotal", round2(variableTotal));
        payload.put("fixedSharePct", expenseTotal > 0 ? round1(fixedTotal * 100.0 / expenseTotal) : 0.0);
        payload.put("variableSharePct", expenseTotal > 0 ? round1(variableTotal * 100.0 / expenseTotal) : 0.0);
        payload.put("metricsSource", "v_transaction_finance_semantics.semantic_tag");
        payload.put("periodStart", start.toString());
        payload.put("periodEnd", end.toString());
        return payload;
    }

    private static double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static LocalDate parseEnd(String toStr) {
        if (toStr == null || toStr.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(toStr.trim(), US);
    }

    private static LocalDate parseStart(String fromStr, LocalDate end) {
        if (fromStr == null || fromStr.isBlank()) {
            return end.minusMonths(11).withDayOfMonth(1);
        }
        return LocalDate.parse(fromStr.trim(), US);
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user.trim().toLowerCase(Locale.ROOT);
    }
}
