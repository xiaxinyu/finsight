package com.finsight.application.finance;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.Transaction;
import com.finsight.infrastructure.mapper.FinancialMapper;
import com.finsight.infrastructure.mapper.TransactionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransferService {

    private final TransactionMapper transactionMapper;
    private final FinancialMapper financialMapper;
    private final AuthenticationFacade authenticationFacade;

    public TransferService(TransactionMapper transactionMapper,
                           FinancialMapper financialMapper,
                           AuthenticationFacade authenticationFacade) {
        this.transactionMapper = transactionMapper;
        this.financialMapper = financialMapper;
        this.authenticationFacade = authenticationFacade;
    }

    @Transactional
    public Map<String, Object> createTransfer(String fromTransactionId, String toTransactionId, String memo) {
        Transaction from = transactionMapper.selectById(fromTransactionId);
        Transaction to = transactionMapper.selectById(toTransactionId);
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both transactions must exist");
        }
        String groupId = UUID.randomUUID().toString();
        financialMapper.markTransactionsTransfer(Arrays.asList(fromTransactionId, toTransactionId), groupId);

        double fromAmt = amountOf(from);
        double toAmt = amountOf(to);
        BigDecimal amount = BigDecimal.valueOf(Math.max(fromAmt, toAmt));

        Map<String, Object> pair = new LinkedHashMap<>();
        pair.put("id", groupId);
        pair.put("fromTransactionId", fromTransactionId);
        pair.put("toTransactionId", toTransactionId);
        pair.put("amount", amount);
        pair.put("transferDate", from.getTransactionDate() != null ? from.getTransactionDate() : new Date());
        pair.put("transferGroupId", groupId);
        pair.put("memo", memo);
        pair.put("createUser", authenticationFacade.getUserName());
        pair.put("createTime", new Date());
        return pair;
    }

    public List<Map<String, Object>> listTransfers() {
        return financialMapper.listTransferGroups();
    }

    private static double amountOf(Transaction t) {
        if (t.getIncomeMoney() != null && t.getIncomeMoney() > 0) {
            return t.getIncomeMoney();
        }
        if (t.getBalanceMoney() != null) {
            return Math.abs(t.getBalanceMoney());
        }
        return 0;
    }
}
