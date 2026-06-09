package com.finsight.application.finance;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.model.TransferPair;
import com.finsight.infrastructure.mapper.FinancialMapper;
import com.finsight.infrastructure.mapper.TransactionMapper;
import com.finsight.infrastructure.mapper.TransferPairMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private final TransactionMapper transactionMapper;
    private final TransferPairMapper transferPairMapper;
    private final FinancialMapper financialMapper;
    private final AuthenticationFacade authenticationFacade;

    public TransferService(TransactionMapper transactionMapper,
                           TransferPairMapper transferPairMapper,
                           FinancialMapper financialMapper,
                           AuthenticationFacade authenticationFacade) {
        this.transactionMapper = transactionMapper;
        this.transferPairMapper = transferPairMapper;
        this.financialMapper = financialMapper;
        this.authenticationFacade = authenticationFacade;
    }

    @Transactional
    public TransferPair createTransfer(String fromTransactionId, String toTransactionId, String memo) {
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

        TransferPair pair = new TransferPair();
        pair.setId(UUID.randomUUID().toString());
        pair.setFromTransactionId(fromTransactionId);
        pair.setToTransactionId(toTransactionId);
        pair.setAmount(amount);
        pair.setTransferDate(from.getTransactionDate() != null ? from.getTransactionDate() : new Date());
        pair.setTransferGroupId(groupId);
        pair.setMemo(memo);
        pair.setDeleted(0);
        pair.setCreateUser(authenticationFacade.getUserName());
        pair.setCreateTime(new Date());
        transferPairMapper.insert(pair);
        return pair;
    }

    public List<TransferPair> listTransfers() {
        return transferPairMapper.selectList(Wrappers.<TransferPair>lambdaQuery()
                .eq(TransferPair::getDeleted, 0)
                .orderByDesc(TransferPair::getTransferDate));
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
