package com.finsight.application.classification;

import com.finsight.application.card.BankCardService;
import com.finsight.application.consume.ClassificationNarrationBuilder;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.application.query.TransactionQuery;
import com.finsight.application.transaction.TransactionReclassificationService;
import com.finsight.domain.model.BankCard;
import com.finsight.domain.model.ClassificationRule;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.web.api.dto.RuleImpactPreviewDto;
import com.finsight.web.api.dto.RuleImpactPreviewRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuleImpactPreviewService {

    public enum ImpactScope {
        UNCLASSIFIED_ONLY,
        WOULD_OVERRIDE,
        ALL_MATCHES
    }

    private static final int TXN_CAP = 3000;
    private static final int SAMPLE_CAP = 50;

    private final TransactionRepository transactionRepository;
    private final ClassificationService classificationService;
    private final ConsumeCategoryService categoryService;
    private final RulePatternMatcher patternMatcher;
    private final BankCardService bankCardService;

    public RuleImpactPreviewService(TransactionRepository transactionRepository,
                                    ClassificationService classificationService,
                                    ConsumeCategoryService categoryService,
                                    RulePatternMatcher patternMatcher,
                                    BankCardService bankCardService) {
        this.transactionRepository = transactionRepository;
        this.classificationService = classificationService;
        this.categoryService = categoryService;
        this.patternMatcher = patternMatcher;
        this.bankCardService = bankCardService;
    }

    public RuleImpactPreviewDto preview(RuleImpactPreviewRequest req) {
        ClassificationRule draft = toDraftRule(req);
        ImpactScope scope = parseScope(req.getScope());
        ConsumeCategory target = resolveTargetCategory(draft.getCategoryId());

        TransactionQuery q = new TransactionQuery();
        Date since = Date.from(LocalDate.now().minusDays(90).atStartOfDay(ZoneId.systemDefault()).toInstant());
        q.setTransactionDateStart(since);
        Page page = new Page(1, TXN_CAP);
        List<Transaction> transactions = transactionRepository.getTransactions(q, page);

        RuleImpactPreviewDto out = new RuleImpactPreviewDto();
        out.setScope(scope.name());
        out.setDraftCategoryId(draft.getCategoryId());
        if (target != null) {
            out.setDraftCategoryCode(target.getCode());
            out.setDraftCategoryName(target.getName());
        }

        Map<String, RuleImpactPreviewDto.CategoryImpactRow> beforeByCat = new LinkedHashMap<>();
        Map<String, RuleImpactPreviewDto.CategoryImpactRow> afterByCat = new LinkedHashMap<>();
        long matchedCount = 0;
        double matchedAmount = 0;
        long unclassifiedMatches = 0;
        long overrideMatches = 0;

        for (Transaction tx : transactions) {
            if (tx == null || isTransfer(tx)) {
                continue;
            }
            BankCard card = resolveCard(tx);
            String bankCode = card != null ? card.getBankCode() : "";
            String cardTypeCode = card != null ? card.getCardTypeCode() : StringUtils.trimToEmpty(tx.getCardTypeName());
            if (!patternMatcher.matchesTransaction(draft, tx, bankCode, cardTypeCode)) {
                continue;
            }
            boolean unclassified = TransactionReclassificationService.isUnclassified(tx);
            String beforeCode = StringUtils.defaultIfBlank(tx.getConsumeCode(), "__UNCLASSIFIED__");
            String beforeName = StringUtils.defaultIfBlank(tx.getConsumeName(), "Unclassified");
            String afterCode = target != null ? StringUtils.defaultIfBlank(target.getCode(), draft.getCategoryId()) : draft.getCategoryId();
            String afterName = target != null ? StringUtils.defaultIfBlank(target.getName(), afterCode) : afterCode;

            boolean wouldOverride = !unclassified && !StringUtils.equalsIgnoreCase(beforeCode, afterCode);
            if (scope == ImpactScope.UNCLASSIFIED_ONLY && !unclassified) {
                continue;
            }
            if (scope == ImpactScope.WOULD_OVERRIDE && !wouldOverride) {
                continue;
            }

            matchedCount++;
            double amt = rowAmount(tx);
            matchedAmount += amt;
            if (unclassified) {
                unclassifiedMatches++;
            }
            if (wouldOverride) {
                overrideMatches++;
            }
            bumpCategory(beforeByCat, beforeCode, beforeName, amt);
            bumpCategory(afterByCat, afterCode, afterName, amt);

            if (out.getSamples().size() < SAMPLE_CAP) {
                RuleImpactPreviewDto.SampleRow sample = buildSample(tx, draft, bankCode, cardTypeCode,
                        beforeCode, beforeName, afterCode, afterName, unclassified, wouldOverride);
                out.getSamples().add(sample);
            }
        }

        out.setMatchedCount(matchedCount);
        out.setMatchedAmount(matchedAmount);
        out.setUnclassifiedMatchCount(unclassifiedMatches);
        out.setWouldOverrideCount(overrideMatches);
        out.setBeforeByCategory(new ArrayList<>(beforeByCat.values()));
        out.getBeforeByCategory().sort(Comparator.comparing(RuleImpactPreviewDto.CategoryImpactRow::getAmount).reversed());
        out.setAfterByCategory(new ArrayList<>(afterByCat.values()));
        out.getAfterByCategory().sort(Comparator.comparing(RuleImpactPreviewDto.CategoryImpactRow::getAmount).reversed());
        return out;
    }

    private RuleImpactPreviewDto.SampleRow buildSample(
            Transaction tx,
            ClassificationRule draft,
            String bankCode,
            String cardTypeCode,
            String beforeCode,
            String beforeName,
            String afterCode,
            String afterName,
            boolean unclassified,
            boolean wouldOverride) {
        String narration = ClassificationNarrationBuilder.fromTransaction(tx);
        double amount = rowAmount(tx);
        Date txnDate = tx.getTransactionDate() != null ? tx.getTransactionDate() : tx.getBookKeepingDate();

        RuleImpactPreviewDto.SampleRow sample = new RuleImpactPreviewDto.SampleRow();
        sample.setTransactionId(tx.getId());
        sample.setTransactionDate(txnDate);
        sample.setDescription(StringUtils.abbreviate(tx.getTransactionDesc(), 120));
        sample.setAmount(amount);
        sample.setBeforeCategoryCode(beforeCode);
        sample.setBeforeCategoryName(beforeName);
        sample.setAfterCategoryCode(afterCode);
        sample.setAfterCategoryName(afterName);
        sample.setUnclassified(unclassified);
        sample.setWouldOverride(wouldOverride);

        List<ClassificationService.Result> topN = classificationService.classifyTopN(
                narration, bankCode, cardTypeCode, amount, txnDate, 3);
        List<RuleImpactPreviewDto.CandidateHit> candidates = new ArrayList<>();
        for (ClassificationService.Result hit : topN) {
            RuleImpactPreviewDto.CandidateHit c = new RuleImpactPreviewDto.CandidateHit();
            c.setCategoryCode(hit.id);
            c.setCategoryName(hit.name);
            c.setPriority(hit.priority);
            c.setWinner(StringUtils.equalsIgnoreCase(hit.id, afterCode));
            candidates.add(c);
        }
        sample.setCandidates(candidates);
        sample.setPriorityExplanation(buildPriorityExplanation(draft, candidates, afterCode));
        return sample;
    }

    private static String buildPriorityExplanation(
            ClassificationRule draft,
            List<RuleImpactPreviewDto.CandidateHit> candidates,
            String afterCode) {
        int draftPri = draft.getPriority() == null ? 999 : draft.getPriority();
        StringBuilder sb = new StringBuilder();
        sb.append("Draft rule priority ").append(draftPri);
        if (candidates.isEmpty()) {
            sb.append("; no engine candidates on this narration");
            return sb.toString();
        }
        RuleImpactPreviewDto.CandidateHit winner = candidates.stream()
                .filter(RuleImpactPreviewDto.CandidateHit::isWinner)
                .findFirst()
                .orElse(candidates.get(0));
        sb.append("; engine winner ").append(winner.getCategoryCode())
                .append(" (pri ").append(winner.getPriority() == null ? "?" : winner.getPriority()).append(")");
        if (StringUtils.equalsIgnoreCase(winner.getCategoryCode(), afterCode)) {
            sb.append(" — aligns with draft target");
        } else {
            sb.append(" — draft target ").append(afterCode).append(" differs; check priority/conflicts");
        }
        return sb.toString();
    }

    private ConsumeCategory resolveTargetCategory(String categoryRef) {
        if (StringUtils.isBlank(categoryRef)) {
            return null;
        }
        for (ConsumeCategory cat : categoryService.listAll()) {
            if (cat == null) {
                continue;
            }
            if (categoryRef.equals(cat.getId()) || categoryRef.equals(cat.getCode())) {
                return cat;
            }
        }
        return null;
    }

    private static ClassificationRule toDraftRule(RuleImpactPreviewRequest req) {
        ClassificationRule rule = new ClassificationRule();
        rule.setId(req.getRuleId());
        rule.setPattern(req.getPattern());
        rule.setPatternType(req.getPatternType());
        rule.setCategoryId(req.getCategoryId());
        rule.setPriority(req.getPriority());
        rule.setActive(1);
        rule.setBankCode(req.getBankCode());
        rule.setCardTypeCode(req.getCardTypeCode());
        rule.setMinAmount(req.getMinAmount());
        rule.setMaxAmount(req.getMaxAmount());
        rule.setStartDate(req.getStartDate());
        rule.setEndDate(req.getEndDate());
        return rule;
    }

    private static ImpactScope parseScope(String scope) {
        if (StringUtils.isBlank(scope)) {
            return ImpactScope.ALL_MATCHES;
        }
        try {
            return ImpactScope.valueOf(scope.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ImpactScope.ALL_MATCHES;
        }
    }

    private BankCard resolveCard(Transaction tx) {
        if (StringUtils.isNotBlank(tx.getBankCardId())) {
            return bankCardService.getById(tx.getBankCardId());
        }
        return null;
    }

    private static boolean isTransfer(Transaction tx) {
        return "transfer".equalsIgnoreCase(StringUtils.trimToEmpty(tx.getTxnKind()));
    }

    private static double rowAmount(Transaction tx) {
        double income = tx.getIncomeMoney() == null ? 0.0 : Math.abs(tx.getIncomeMoney());
        double expense = tx.getBalanceMoney() == null ? 0.0 : Math.abs(tx.getBalanceMoney());
        return Math.max(income, expense);
    }

    private static void bumpCategory(Map<String, RuleImpactPreviewDto.CategoryImpactRow> map,
                                       String code, String name, double amount) {
        RuleImpactPreviewDto.CategoryImpactRow row = map.computeIfAbsent(code, k -> {
            RuleImpactPreviewDto.CategoryImpactRow r = new RuleImpactPreviewDto.CategoryImpactRow();
            r.setCategoryCode(code);
            r.setCategoryName(name);
            return r;
        });
        row.setTxnCount(row.getTxnCount() + 1);
        row.setAmount(row.getAmount() + amount);
    }
}
