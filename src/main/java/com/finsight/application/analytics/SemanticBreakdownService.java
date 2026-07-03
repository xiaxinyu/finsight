package com.finsight.application.analytics;

import com.finsight.application.authentication.LedgerUserScope;
import com.finsight.application.classification.FinanceSemanticsCatalog;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final LedgerUserScope ledgerUserScope;

    public SemanticBreakdownService(SemanticBreakdownRepository breakdownRepository,
            LedgerUserScope ledgerUserScope) {
        this.breakdownRepository = breakdownRepository;
        this.ledgerUserScope = ledgerUserScope;
    }

    public Map<String, Object> expenseBreakdown(String fromStr, String toStr, String cardId, String consumeId) {
        return breakdown(fromStr, toStr, cardId, consumeId, SemanticBreakdownRepository.BreakdownScope.EXPENSE);
    }

    public Map<String, Object> breakdown(String fromStr, String toStr, String cardId, String consumeId, String scope) {
        SemanticBreakdownRepository.BreakdownScope parsed = parseScope(scope);
        return breakdown(fromStr, toStr, cardId, consumeId, parsed);
    }

    public Map<String, Object> breakdown(
            String fromStr,
            String toStr,
            String cardId,
            String consumeId,
            SemanticBreakdownRepository.BreakdownScope scope) {
        LocalDate end = parseEnd(toStr);
        LocalDate start = parseStart(fromStr, end);

        List<SemanticBreakdownRepository.TagAmountRow> raw = breakdownRepository.bySemanticTag(
                new SemanticBreakdownRepository.SemanticBreakdownQuery(
                        userKey(), start, end, cardId, consumeId, scope));

        double periodTotal = raw.stream().mapToDouble(SemanticBreakdownRepository.TagAmountRow::amount).sum();
        double fixedTotal = 0;
        double variableTotal = 0;

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SemanticBreakdownRepository.TagAmountRow row : raw) {
            String group = FinanceSemanticsCatalog.semanticTagGroup(row.tagId());
            if (scope == SemanticBreakdownRepository.BreakdownScope.EXPENSE) {
                if ("fixed".equals(group)) {
                    fixedTotal += row.amount();
                } else if ("expense".equals(group) || "other".equals(group)) {
                    variableTotal += row.amount();
                }
            }
            rows.add(toRow(row, periodTotal));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scope", scope.name().toLowerCase(Locale.ROOT));
        payload.put("rows", rows);
        payload.put("periodTotal", round2(periodTotal));
        payload.put("expenseTotal", round2(scope == SemanticBreakdownRepository.BreakdownScope.EXPENSE
                ? periodTotal : 0));
        payload.put("fixedTotal", round2(fixedTotal));
        payload.put("variableTotal", round2(variableTotal));
        payload.put("fixedSharePct", periodTotal > 0 && scope == SemanticBreakdownRepository.BreakdownScope.EXPENSE
                ? round1(fixedTotal * 100.0 / periodTotal) : 0.0);
        payload.put("variableSharePct", periodTotal > 0 && scope == SemanticBreakdownRepository.BreakdownScope.EXPENSE
                ? round1(variableTotal * 100.0 / periodTotal) : 0.0);
        payload.put("metricsSource", FinanceSemanticsCatalog.catalogPayload().get("metricsSourceLabel"));
        payload.put("periodStart", start.toString());
        payload.put("periodEnd", end.toString());
        return payload;
    }

    private static Map<String, Object> toRow(SemanticBreakdownRepository.TagAmountRow row, double periodTotal) {
        String classL1 = FinanceSemanticsCatalog.semanticTagPickerRowLabel(row.tagId());
        String classL2 = FinanceSemanticsCatalog.semanticTagLevel2Label(row.tagId());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tagId", row.tagId());
        out.put("classL1", classL1);
        out.put("classL2", classL2);
        out.put("classification", FinanceSemanticsCatalog.semanticTagClassification(row.tagId()));
        out.put("txnType", FinanceSemanticsCatalog.semanticTagTxnTypeLabel(row.tagId()));
        out.put("label", classL2);
        out.put("group", FinanceSemanticsCatalog.semanticTagGroup(row.tagId()));
        out.put("amount", round2(row.amount()));
        out.put("sharePct", periodTotal > 0 ? round1(row.amount() * 100.0 / periodTotal) : 0.0);
        return out;
    }

    private static SemanticBreakdownRepository.BreakdownScope parseScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return SemanticBreakdownRepository.BreakdownScope.EXPENSE;
        }
        return switch (scope.trim().toLowerCase(Locale.ROOT)) {
            case "income" -> SemanticBreakdownRepository.BreakdownScope.INCOME;
            case "non_pnl", "non-pnl", "capital", "finance" -> SemanticBreakdownRepository.BreakdownScope.NON_PNL;
            case "tax" -> SemanticBreakdownRepository.BreakdownScope.TAX;
            case "refund" -> SemanticBreakdownRepository.BreakdownScope.REFUND;
            case "all" -> SemanticBreakdownRepository.BreakdownScope.ALL;
            default -> SemanticBreakdownRepository.BreakdownScope.EXPENSE;
        };
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
        return ledgerUserScope.resolve();
    }
}
