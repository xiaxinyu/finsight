package com.finsight.application.classification;

import com.finsight.application.consume.ClassificationNarrationBuilder;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.consume.ClassificationTextNormalizer;
import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.application.query.TransactionQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Proactive category suggestions when the strict rule engine returns no match.
 */
@Service
public class ClassificationRecommendationService {

    private static final int SIMILAR_TXN_CAP = 200;
    private static final int MIN_SIMILAR_COUNT = 2;

    private final ClassificationService classificationService;
    private final TransactionRepository transactionRepository;
    private final ConsumeCategoryService categoryService;

    public ClassificationRecommendationService(ClassificationService classificationService,
                                               TransactionRepository transactionRepository,
                                               ConsumeCategoryService categoryService) {
        this.classificationService = classificationService;
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    public Optional<CategoryRecommendation> recommend(Transaction tx, String bankCode, String cardTypeCode) {
        if (tx == null) {
            return Optional.empty();
        }
        List<String> keywords = extractKeywords(tx);
        CategoryRecommendation best = null;

        CategoryRecommendation weakRule = recommendFromWeakRules(tx, keywords, bankCode, cardTypeCode);
        if (weakRule != null) {
            best = weakRule;
        }

        CategoryRecommendation similar = recommendFromSimilarTransactions(tx, keywords);
        if (similar != null && (best == null || similar.getConfidence() > best.getConfidence())) {
            best = similar;
        }

        CategoryRecommendation heuristic = recommendFromHeuristic(tx, keywords);
        if (heuristic != null && (best == null || heuristic.getConfidence() > best.getConfidence())) {
            best = heuristic;
        }

        if (best != null && best.hasCategory()) {
            if (best.getSuggestedKeywords().isEmpty()) {
                best.getSuggestedKeywords().addAll(keywords);
            }
            return Optional.of(best);
        }

        if (!keywords.isEmpty()) {
            return Optional.of(CategoryRecommendation.keywordsOnly(
                    keywords,
                    "No rule matched — consider adding a contains rule for: "
                            + String.join(", ", keywords.subList(0, Math.min(3, keywords.size())))));
        }
        return Optional.empty();
    }

    private CategoryRecommendation recommendFromWeakRules(Transaction tx, List<String> keywords,
                                                            String bankCode, String cardTypeCode) {
        String narration = ClassificationNarrationBuilder.fromTransaction(tx);
        List<ClassificationService.Result> hits = classificationService.suggestRelaxed(
                narration,
                StringUtils.defaultString(bankCode),
                StringUtils.defaultString(cardTypeCode),
                amount(tx),
                txnDate(tx),
                3);
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        for (ClassificationService.Result hit : hits) {
            if (hit == null || isOtherCategory(hit.id, hit.name)) {
                continue;
            }
            CategoryRecommendation rec = new CategoryRecommendation();
            rec.setCategoryCode(hit.id);
            rec.setCategoryName(hit.name);
            rec.setSource(CategoryRecommendation.Source.WEAK_RULE);
            rec.setConfidence(55);
            rec.setReason("Partial keyword overlap with existing rules");
            rec.getSuggestedKeywords().addAll(keywords);
            return rec;
        }
        return null;
    }

    private CategoryRecommendation recommendFromSimilarTransactions(Transaction tx, List<String> keywords) {
        for (String phrase : searchPhrases(tx)) {
            TransactionQuery q = new TransactionQuery();
            q.setDemoArea(phrase);
            List<Transaction> hits = transactionRepository.getTransactions(q, new Page(1, SIMILAR_TXN_CAP));
            if (hits == null || hits.isEmpty()) {
                continue;
            }
            Map<String, CategoryVote> votes = new HashMap<>();
            for (Transaction other : hits) {
                if (other == null || tx.getId() != null && tx.getId().equals(other.getId())) {
                    continue;
                }
                if (isUnclassified(other)) {
                    continue;
                }
                String code = firstNonBlank(other.getConsumeCode(), other.getCategoryCode());
                String name = firstNonBlank(other.getConsumeName(), other.getCategoryName());
                if (StringUtils.isBlank(code) || isOtherCategory(code, name)) {
                    continue;
                }
                votes.computeIfAbsent(code, k -> new CategoryVote(code, name)).count++;
            }
            if (votes.isEmpty()) {
                continue;
            }
            CategoryVote winner = votes.values().stream()
                    .max(Comparator.comparingInt(v -> v.count))
                    .orElse(null);
            if (winner == null || winner.count < MIN_SIMILAR_COUNT) {
                continue;
            }
            CategoryRecommendation rec = new CategoryRecommendation();
            rec.setCategoryCode(winner.code);
            rec.setCategoryName(winner.name);
            rec.setSource(CategoryRecommendation.Source.SIMILAR);
            rec.setConfidence(Math.min(95, 50 + winner.count * 5));
            rec.setReason(winner.count + " classified transactions contain 「" + phrase + "」 → " + winner.name);
            rec.getSuggestedKeywords().add(phrase);
            rec.getSuggestedKeywords().addAll(keywords);
            return rec;
        }
        return null;
    }

    private CategoryRecommendation recommendFromHeuristic(Transaction tx, List<String> keywords) {
        String narration = ClassificationNarrationBuilder.fromTransaction(tx);
        String expanded = ClassificationTextNormalizer.expand(narration).toLowerCase(Locale.ROOT);
        String[][] hints = {
                {"邮购分期", "分期", "SHOP", "购物", "网购", "邮购"},
                {"年费", "手续费", "FEE", "银行", "金融"},
                {"代付", "TRANSFER", "转账"},
                {"地铁", "公交", "滴滴", "TRAVEL", "交通", "出行"},
                {"美团", "饿了么", "外卖", "FOOD", "餐", "食"},
        };
        for (String[] row : hints) {
            boolean matched = false;
            for (int i = 0; i < 3 && i < row.length; i++) {
                if (expanded.contains(row[i].toLowerCase(Locale.ROOT))) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                continue;
            }
            ConsumeCategory cat = findCategoryByHints(row, 3);
            if (cat == null) {
                continue;
            }
            CategoryRecommendation rec = new CategoryRecommendation();
            rec.setCategoryCode(cat.getCode());
            rec.setCategoryName(cat.getName());
            rec.setSource(CategoryRecommendation.Source.HEURISTIC);
            rec.setConfidence(45);
            rec.setReason("Description pattern suggests 「" + cat.getName() + "」");
            rec.getSuggestedKeywords().addAll(keywords);
            return rec;
        }
        return null;
    }

    private ConsumeCategory findCategoryByHints(String[] row, int hintStart) {
        List<ConsumeCategory> all = categoryService.listAll();
        if (all == null) {
            return null;
        }
        for (int i = hintStart; i < row.length; i++) {
            String hint = row[i];
            for (ConsumeCategory c : all) {
                if (c == null || isOtherCategory(c.getCode(), c.getName())) {
                    continue;
                }
                String code = StringUtils.defaultString(c.getCode()).toUpperCase(Locale.ROOT);
                String name = StringUtils.defaultString(c.getName());
                if (code.contains(hint.toUpperCase(Locale.ROOT)) || name.contains(hint)) {
                    return c;
                }
            }
        }
        return null;
    }

    private List<String> searchPhrases(Transaction tx) {
        String narration = ClassificationNarrationBuilder.fromTransaction(tx);
        Set<String> phrases = new LinkedHashSet<>();
        String expanded = ClassificationTextNormalizer.expand(narration);
        for (String part : expanded.split("\\s+")) {
            String p = StringUtils.trimToEmpty(part);
            if (p.length() >= 2 && containsChinese(p)) {
                phrases.add(p);
            }
        }
        if (StringUtils.isNotBlank(narration)) {
            phrases.add(narration.trim());
        }
        List<String> sorted = new ArrayList<>(phrases);
        sorted.sort(Comparator.comparingInt(String::length).reversed());
        return sorted;
    }

    private List<String> extractKeywords(Transaction tx) {
        String narration = ClassificationNarrationBuilder.fromTransaction(tx);
        List<String> tokens = classificationService.tokens(narration);
        Set<String> out = new LinkedHashSet<>();
        if (tokens != null) {
            for (String t : tokens) {
                if (StringUtils.isNotBlank(t) && t.length() >= 2) {
                    out.add(t.trim());
                }
            }
        }
        for (String phrase : searchPhrases(tx)) {
            if (phrase.length() >= 2 && phrase.length() <= 16) {
                out.add(phrase);
            }
        }
        List<String> list = new ArrayList<>(out);
        list.sort(Comparator.comparingInt(String::length).reversed());
        return list.subList(0, Math.min(list.size(), 8));
    }

    private static boolean containsChinese(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.UnicodeScript.of(s.charAt(i)) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOtherCategory(String code, String name) {
        String c = StringUtils.trimToEmpty(code).toUpperCase(Locale.ROOT);
        String n = StringUtils.trimToEmpty(name);
        return c.startsWith("OTHER") || "无法归类的支出".equals(n) || "Uncategorized".equalsIgnoreCase(n);
    }

    private static String firstNonBlank(String a, String b) {
        return StringUtils.isNotBlank(a) ? a.trim() : StringUtils.trimToEmpty(b);
    }

    private static double amount(Transaction tx) {
        double income = tx.getIncomeMoney() == null ? 0.0 : Math.max(0.0, tx.getIncomeMoney());
        double expense = tx.getBalanceMoney() == null ? 0.0 : Math.max(0.0, tx.getBalanceMoney());
        return Math.max(income, expense);
    }

    private static java.util.Date txnDate(Transaction tx) {
        return tx.getTransactionDate() != null ? tx.getTransactionDate() : tx.getBookKeepingDate();
    }

    private static boolean isUnclassified(Transaction t) {
        return StringUtils.isBlank(t.getConsumeCode())
                && StringUtils.isBlank(t.getConsumeName())
                && StringUtils.isBlank(t.getCategoryCode())
                && StringUtils.isBlank(t.getCategoryName());
    }

    private static final class CategoryVote {
        private final String code;
        private final String name;
        private int count;

        private CategoryVote(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
