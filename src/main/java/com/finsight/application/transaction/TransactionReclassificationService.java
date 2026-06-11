package com.finsight.application.transaction;

import com.finsight.application.card.BankCardService;
import com.finsight.application.consume.ClassificationNarrationBuilder;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.query.TransactionQuery;
import com.finsight.application.query.TransactionQueryAssembler;
import com.finsight.application.support.TransactionIdList;
import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.BankCard;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.web.api.dto.TransactionParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Re-runs the rule engine against ledger rows. Defaults are conservative: only unclassified rows,
 * no overwrite of existing categories, no OTHER fallback unless explicitly requested.
 */
@Service
public class TransactionReclassificationService {

    private static final int BATCH_CAP = 5000;

    private final TransactionRepository transactionRepository;
    private final ClassificationService classificationService;
    private final BankCardService bankCardService;
    private final ITransactionService transactionService;

    public TransactionReclassificationService(TransactionRepository transactionRepository,
                                                ClassificationService classificationService,
                                                BankCardService bankCardService,
                                                ITransactionService transactionService) {
        this.transactionRepository = transactionRepository;
        this.classificationService = classificationService;
        this.bankCardService = bankCardService;
        this.transactionService = transactionService;
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionReclassificationResult reclassify(String idsCsv,
                                                        boolean persist,
                                                        boolean overrideExisting,
                                                        boolean useOtherFallback,
                                                        String userName) {
        List<String> ids = TransactionIdList.parseCommaSeparatedIds(idsCsv);
        TransactionReclassificationResult result = new TransactionReclassificationResult();
        result.setDryRun(!persist);
        result.setRequested(ids.size());

        for (String id : ids) {
            if (StringUtils.isBlank(id)) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            Transaction tx = transactionRepository.selectById(id.trim());
            if (tx == null || isDeleted(tx)) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            if ("transfer".equalsIgnoreCase(StringUtils.trimToEmpty(tx.getTxnKind()))) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            if (!overrideExisting && !isUnclassified(tx)) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }

            ClassificationService.Result match = resolveCategory(tx, useOtherFallback);
            if (match == null || StringUtils.isBlank(match.id)) {
                result.setNoMatch(result.getNoMatch() + 1);
                continue;
            }

            result.addPreview(tx.getId(), match.id, match.name, persist ? "APPLY" : "PREVIEW");
            if (persist) {
                applyCategory(tx, match, userName);
                result.setClassified(result.getClassified() + 1);
            } else {
                result.setClassified(result.getClassified() + 1);
            }
        }
        return result;
    }

    /**
     * Re-runs the rule engine on unclassified rows matching list filters (date, card, keyword, etc.).
     */
    @Transactional(rollbackFor = Exception.class)
    public TransactionReclassificationResult reclassifyUnclassified(TransactionParam param,
                                                                    boolean persist,
                                                                    boolean useOtherFallback,
                                                                    String userName) throws AppServiceException {
        TransactionQuery q = TransactionQueryAssembler.from(param);
        q.setEmptyConsume(Boolean.TRUE);
        Page page = new Page(1, BATCH_CAP);
        List<Transaction> list = transactionRepository.getTransactions(q, page);
        String ids = list.stream()
                .map(Transaction::getId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(","));
        if (StringUtils.isBlank(ids)) {
            TransactionReclassificationResult empty = new TransactionReclassificationResult();
            empty.setDryRun(!persist);
            return empty;
        }
        return reclassify(ids, persist, false, useOtherFallback, userName);
    }

    private ClassificationService.Result resolveCategory(Transaction tx, boolean useOtherFallback) {
        String narration = ClassificationNarrationBuilder.fromTransaction(tx);
        BankCard card = resolveCard(tx);
        String bankCode = card != null ? card.getBankCode() : "";
        String cardTypeCode = card != null ? card.getCardTypeCode() : StringUtils.trimToEmpty(tx.getCardTypeName());
        double amount = resolveAmount(tx);
        Date txnDate = tx.getTransactionDate() != null ? tx.getTransactionDate() : tx.getBookKeepingDate();

        ClassificationService.Result match = classificationService.classify(
                narration, bankCode, cardTypeCode, amount, txnDate);
        if (match == null && useOtherFallback) {
            match = classificationService.otherFallback();
        }
        return match;
    }

    private void applyCategory(Transaction tx, ClassificationService.Result match, String userName) {
        tx.setCategoryCode(match.id);
        tx.setCategoryName(match.name);
        tx.setConsumeCode(match.id);
        tx.setConsumeName(match.name);
        try {
            transactionService.updateTransaction(tx, userName);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update transaction " + tx.getId() + ": " + e.getMessage(), e);
        }
    }

    static boolean isUnclassified(Transaction t) {
        return StringUtils.isBlank(t.getConsumeCode())
                && StringUtils.isBlank(t.getConsumeName())
                && StringUtils.isBlank(t.getCategoryCode())
                && StringUtils.isBlank(t.getCategoryName());
    }

    private static boolean isDeleted(Transaction t) {
        return t.getDeleted() != null && t.getDeleted() == 1;
    }

    private static double resolveAmount(Transaction t) {
        double income = t.getIncomeMoney() == null ? 0.0 : Math.max(0.0, t.getIncomeMoney());
        double expense = t.getBalanceMoney() == null ? 0.0 : Math.max(0.0, t.getBalanceMoney());
        return Math.max(income, expense);
    }

    private BankCard resolveCard(Transaction tx) {
        if (StringUtils.isNotBlank(tx.getBankCardId())) {
            BankCard card = bankCardService.getById(tx.getBankCardId());
            if (card != null) {
                return card;
            }
        }
        return null;
    }
}
