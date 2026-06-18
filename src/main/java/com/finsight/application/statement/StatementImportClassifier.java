package com.finsight.application.statement;

import com.finsight.application.consume.ClassificationNarrationBuilder;
import com.finsight.application.consume.ClassificationProperties;
import com.finsight.application.consume.ClassificationService;
import com.finsight.domain.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

/**
 * Classifies parsed statement rows using merchant-clean narration plus import heuristics.
 */
@Component
public class StatementImportClassifier {

    private final ClassificationService classificationService;
    private final ClassificationProperties classificationProperties;
    private final ImportCategoryHeuristic importHeuristic;

    public StatementImportClassifier(ClassificationService classificationService,
                                     ClassificationProperties classificationProperties,
                                     ImportCategoryHeuristic importHeuristic) {
        this.classificationService = classificationService;
        this.classificationProperties = classificationProperties;
        this.importHeuristic = importHeuristic;
    }

    public void classify(Transaction t, String bankCode, String cardTypeCode) {
        double amount = amount(t);
        Date txnDate = txnDate(t);
        String narration = ClassificationNarrationBuilder.forMatching(t);

        ClassificationService.Result rule = classificationService.classify(
                narration, bankCode, cardTypeCode, amount, txnDate);
        Optional<ImportCategoryHeuristic.Match> heuristic = importHeuristic.match(narration, amount);

        ClassificationService.Result chosen = choose(rule, heuristic.orElse(null), narration);
        if (chosen == null && classificationProperties.isImportOtherFallback()) {
            chosen = classificationService.otherFallback();
        }
        apply(t, chosen);
    }

    private ClassificationService.Result choose(ClassificationService.Result rule,
                                                ImportCategoryHeuristic.Match heuristic,
                                                String narration) {
        if (heuristic != null) {
            if (rule == null) {
                return toResult(heuristic);
            }
            if (importHeuristic.shouldOverrideRule(rule.id, rule.name, heuristic, narration)) {
                return toResult(heuristic);
            }
        }
        return rule;
    }

    private static ClassificationService.Result toResult(ImportCategoryHeuristic.Match match) {
        ClassificationService.Result r = new ClassificationService.Result();
        r.id = match.categoryCode();
        r.name = match.categoryName();
        return r;
    }

    private static void apply(Transaction t, ClassificationService.Result r) {
        if (r == null) {
            return;
        }
        t.setCategoryCode(r.id);
        t.setCategoryName(r.name);
        t.setConsumeCode(r.id);
        t.setConsumeName(r.name);
    }

    private static double amount(Transaction t) {
        double income = t.getIncomeMoney() == null ? 0.0 : Math.max(0.0, t.getIncomeMoney());
        double expense = t.getBalanceMoney() == null ? 0.0 : Math.max(0.0, t.getBalanceMoney());
        return Math.max(income, expense);
    }

    private static Date txnDate(Transaction t) {
        if (t.getTransactionDate() != null) {
            return t.getTransactionDate();
        }
        return t.getBookKeepingDate();
    }
}
