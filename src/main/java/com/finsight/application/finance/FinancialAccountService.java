package com.finsight.application.finance;

import com.finsight.application.card.BankCardService;
import com.finsight.domain.model.BankCard;
import com.finsight.domain.model.KeyValue;
import com.finsight.infrastructure.mapper.FinancialMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinancialAccountService {

    private final BankCardService bankCardService;
    private final FinancialMapper financialMapper;

    public FinancialAccountService(BankCardService bankCardService, FinancialMapper financialMapper) {
        this.bankCardService = bankCardService;
        this.financialMapper = financialMapper;
    }

    public List<Map<String, Object>> listAccounts() {
        List<BankCard> cards = bankCardService.listAllEnabled();
        List<Map<String, Object>> out = new ArrayList<>();
        for (BankCard card : cards) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", card.getId());
            row.put("name", displayCardName(card));
            row.put("accountType", mapCardType(card.getCardTypeCode()));
            row.put("bankCardId", card.getId());
            row.put("currency", "CNY");
            row.put("isLiability", "credit".equalsIgnoreCase(card.getCardTypeCode()) ? 1 : 0);
            out.add(row);
        }
        return out;
    }

    public List<KeyValue> latestBalances() {
        return financialMapper.latestBalancesFromBankCards();
    }

    public Map<String, Object> recordSnapshot(String bankCardId, Date date, BigDecimal balance, String source) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("accountId", bankCardId);
        snap.put("snapshotDate", date);
        snap.put("balance", balance);
        snap.put("source", source);
        return snap;
    }

    private static String displayCardName(BankCard card) {
        if (card.getCardName() != null && !card.getCardName().isBlank()) {
            return card.getCardName();
        }
        return card.getBankCode() + " " + card.getCardTypeCode();
    }

    private static String mapCardType(String cardTypeCode) {
        if ("credit".equalsIgnoreCase(cardTypeCode)) {
            return "credit";
        }
        return "debit";
    }
}
