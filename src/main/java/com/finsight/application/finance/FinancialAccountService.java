package com.finsight.application.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.card.BankCardService;
import com.finsight.domain.model.AccountBalanceSnapshot;
import com.finsight.domain.model.BankCard;
import com.finsight.domain.model.FinancialAccount;
import com.finsight.infrastructure.mapper.AccountBalanceSnapshotMapper;
import com.finsight.infrastructure.mapper.FinancialAccountMapper;
import com.finsight.infrastructure.mapper.FinancialMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class FinancialAccountService {

    private final FinancialAccountMapper accountMapper;
    private final AccountBalanceSnapshotMapper snapshotMapper;
    private final BankCardService bankCardService;
    private final FinancialMapper financialMapper;
    private final AuthenticationFacade authenticationFacade;

    public FinancialAccountService(FinancialAccountMapper accountMapper,
                                   AccountBalanceSnapshotMapper snapshotMapper,
                                   BankCardService bankCardService,
                                   FinancialMapper financialMapper,
                                   AuthenticationFacade authenticationFacade) {
        this.accountMapper = accountMapper;
        this.snapshotMapper = snapshotMapper;
        this.bankCardService = bankCardService;
        this.financialMapper = financialMapper;
        this.authenticationFacade = authenticationFacade;
    }

    public List<FinancialAccount> listAccounts() {
        syncFromBankCardsIfEmpty();
        LambdaQueryWrapper<FinancialAccount> q = Wrappers.lambdaQuery();
        q.eq(FinancialAccount::getDeleted, 0).orderByAsc(FinancialAccount::getDisplayOrder);
        return accountMapper.selectList(q);
    }

    @Transactional
    public void syncFromBankCardsIfEmpty() {
        Long count = accountMapper.selectCount(Wrappers.<FinancialAccount>lambdaQuery().eq(FinancialAccount::getDeleted, 0));
        if (count != null && count > 0) {
            return;
        }
        List<BankCard> cards = bankCardService.listAllEnabled();
        int order = 0;
        for (BankCard card : cards) {
            FinancialAccount fa = new FinancialAccount();
            fa.setId(UUID.randomUUID().toString());
            fa.setName(displayCardName(card));
            fa.setAccountType(mapCardType(card.getCardTypeCode()));
            fa.setBankCardId(card.getId());
            fa.setCurrency("CNY");
            fa.setIsLiability("credit".equalsIgnoreCase(card.getCardTypeCode()) ? 1 : 0);
            fa.setDisplayOrder(order++);
            fa.setDeleted(0);
            fa.setCreateUser(authenticationFacade.getUserName());
            fa.setCreateTime(new Date());
            accountMapper.insert(fa);
        }
        ensureWalletAccounts();
    }

    private void ensureWalletAccounts() {
        createIfMissing("Alipay", "ewallet", 100);
        createIfMissing("WeChat Pay", "ewallet", 101);
        createIfMissing("Provident Fund", "benefit", 200);
        createIfMissing("Social Insurance", "benefit", 201);
    }

    private void createIfMissing(String name, String type, int order) {
        Long exists = accountMapper.selectCount(Wrappers.<FinancialAccount>lambdaQuery()
                .eq(FinancialAccount::getName, name).eq(FinancialAccount::getDeleted, 0));
        if (exists != null && exists > 0) {
            return;
        }
        FinancialAccount fa = new FinancialAccount();
        fa.setId(UUID.randomUUID().toString());
        fa.setName(name);
        fa.setAccountType(type);
        fa.setCurrency("CNY");
        fa.setIsLiability(0);
        fa.setDisplayOrder(order);
        fa.setDeleted(0);
        fa.setCreateUser(authenticationFacade.getUserName());
        fa.setCreateTime(new Date());
        accountMapper.insert(fa);
    }

    @Transactional
    public AccountBalanceSnapshot recordSnapshot(String accountId, Date date, BigDecimal balance, String source) {
        AccountBalanceSnapshot snap = new AccountBalanceSnapshot();
        snap.setId(UUID.randomUUID().toString());
        snap.setAccountId(accountId);
        snap.setSnapshotDate(date);
        snap.setBalance(balance);
        snap.setSource(source);
        snap.setCreateUser(authenticationFacade.getUserName());
        snap.setCreateTime(new Date());
        snapshotMapper.insert(snap);
        return snap;
    }

    public List<com.finsight.domain.model.KeyValue> latestBalances() {
        syncFromBankCardsIfEmpty();
        return financialMapper.latestAccountBalances();
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
