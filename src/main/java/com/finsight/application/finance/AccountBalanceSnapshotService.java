package com.finsight.application.finance;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.authentication.LedgerUserScope;
import com.finsight.domain.model.Transaction;
import com.finsight.infrastructure.mapper.AccountBalanceSnapshotMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountBalanceSnapshotService {

    private final AccountBalanceSnapshotMapper mapper;
    private final AuthenticationFacade authenticationFacade;
    private final LedgerUserScope ledgerUserScope;

    public AccountBalanceSnapshotService(AccountBalanceSnapshotMapper mapper,
                                         AuthenticationFacade authenticationFacade,
                                         LedgerUserScope ledgerUserScope) {
        this.mapper = mapper;
        this.authenticationFacade = authenticationFacade;
        this.ledgerUserScope = ledgerUserScope;
    }

    public Map<String, Object> recordManual(String bankCardId, Date date, BigDecimal balance) {
        String userId = ledgerUserScope.resolve();
        upsert(userId, bankCardId, date, balance, "manual");
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("accountId", bankCardId);
        snap.put("snapshotDate", date);
        snap.put("balance", balance);
        snap.put("source", "manual");
        return snap;
    }

    public void recordFromTransactions(List<Transaction> transactions, String userName) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }
        String userId = StringUtils.isNotBlank(userName) ? userName.trim() : ledgerUserScope.resolve();
        Map<String, Transaction> latestPerCardDay = new LinkedHashMap<>();
        for (Transaction txn : transactions) {
            if (txn == null || txn.getAccountBalance() == null || txn.getTransactionDate() == null) {
                continue;
            }
            if (StringUtils.isBlank(txn.getBankCardId())) {
                continue;
            }
            String dayKey = txn.getBankCardId() + "|" + dayKey(txn.getTransactionDate());
            latestPerCardDay.merge(dayKey, txn, (a, b) -> compareTxn(a, b) >= 0 ? a : b);
        }
        String actor = StringUtils.defaultIfBlank(userName, authenticationFacade.getUserName());
        for (Transaction txn : latestPerCardDay.values()) {
            upsert(
                    userId,
                    txn.getBankCardId(),
                    txn.getTransactionDate(),
                    BigDecimal.valueOf(txn.getAccountBalance()).setScale(2, RoundingMode.HALF_UP),
                    "import");
        }
    }

    private void upsert(String userId, String cardId, Date snapshotDate, BigDecimal balance, String source) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(cardId) || snapshotDate == null || balance == null) {
            return;
        }
        String actor = authenticationFacade.getUserName();
        mapper.upsertSnapshot(
                UUID.randomUUID().toString(),
                userId,
                cardId,
                balance,
                snapshotDate,
                source,
                actor);
    }

    private static String dayKey(Date date) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private static int compareTxn(Transaction a, Transaction b) {
        int byId = StringUtils.compare(a.getId(), b.getId());
        if (byId != 0) {
            return byId;
        }
        return 0;
    }
}
