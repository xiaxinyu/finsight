package com.finsight.application.classification;

import com.finsight.application.consume.ClassificationNarrationBuilder;
import com.finsight.application.consume.ClassificationTextNormalizer;
import com.finsight.domain.model.ClassificationRule;
import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Matches a single draft/active rule against transaction narration (mirrors DecisionTreeClassifier paths).
 */
@Component
public class RulePatternMatcher {

    public boolean matches(ClassificationRule rule, String narration, String bankCode, String cardTypeCode,
                           Double amount, Date txnDate) {
        if (rule == null || StringUtils.isBlank(rule.getPattern())) {
            return false;
        }
        if (!matchContext(rule, bankCode, cardTypeCode, amount, txnDate)) {
            return false;
        }
        String text = normalize(ClassificationTextNormalizer.expand(narration));
        if (StringUtils.isBlank(text)) {
            return false;
        }
        String type = StringUtils.defaultIfBlank(rule.getPatternType(), "contains").toLowerCase(Locale.ROOT);
        String pat = normalize(rule.getPattern());
        return switch (type) {
            case "equals" -> text.equals(pat);
            case "regex" -> {
                try {
                    yield Pattern.compile(rule.getPattern()).matcher(text).find();
                } catch (Exception e) {
                    yield false;
                }
            }
            case "startswith" -> StringUtils.isNotBlank(pat)
                    && (text.startsWith(pat) || text.contains(" " + pat));
            default -> StringUtils.isNotBlank(pat) && text.contains(pat);
        };
    }

    public boolean matchesTransaction(ClassificationRule rule, Transaction tx, String bankCode, String cardTypeCode) {
        String narration = ClassificationNarrationBuilder.fromTransaction(tx);
        double amount = resolveAmount(tx);
        Date txnDate = tx.getTransactionDate() != null ? tx.getTransactionDate() : tx.getBookKeepingDate();
        return matches(rule, narration, bankCode, cardTypeCode, amount, txnDate);
    }

    private static boolean matchContext(ClassificationRule rule, String bankCode, String cardTypeCode,
                                          Double amount, Date txnDate) {
        if (StringUtils.isNotBlank(rule.getBankCode()) && StringUtils.isNotBlank(bankCode)
                && !rule.getBankCode().equalsIgnoreCase(bankCode.trim())) {
            return false;
        }
        if (StringUtils.isNotBlank(rule.getCardTypeCode()) && StringUtils.isNotBlank(cardTypeCode)
                && !rule.getCardTypeCode().equalsIgnoreCase(cardTypeCode.trim())) {
            return false;
        }
        if (amount != null) {
            if (rule.getMinAmount() != null && amount < rule.getMinAmount()) {
                return false;
            }
            if (rule.getMaxAmount() != null && amount > rule.getMaxAmount()) {
                return false;
            }
        }
        if (txnDate != null) {
            if (rule.getStartDate() != null && txnDate.before(rule.getStartDate())) {
                return false;
            }
            if (rule.getEndDate() != null && txnDate.after(rule.getEndDate())) {
                return false;
            }
        }
        return true;
    }

    private static double resolveAmount(Transaction t) {
        double income = t.getIncomeMoney() == null ? 0.0 : Math.max(0.0, t.getIncomeMoney());
        double expense = t.getBalanceMoney() == null ? 0.0 : Math.max(0.0, t.getBalanceMoney());
        return Math.max(income, expense);
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String t = s.toLowerCase(Locale.ROOT);
        t = t.replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }
}
