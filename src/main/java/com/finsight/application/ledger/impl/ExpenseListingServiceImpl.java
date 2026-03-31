package com.finsight.application.ledger.impl;

import com.finsight.application.ledger.IExpenseListingService;
import com.finsight.application.support.ListingDateSupport;
import com.finsight.core.AppServiceException;
import com.finsight.core.StringTool;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.application.query.TransactionQuery;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.TransactionParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Expense ledger: query all transactions tagged as expense.
 */
@Service
public class ExpenseListingServiceImpl implements IExpenseListingService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public CollectionResult<Transaction> listExpenseTransactions(TransactionParam param) throws AppServiceException {
        TransactionQuery q = new TransactionQuery();
        java.util.Date[] range = ListingDateSupport.parseMmDdYyyyOrDefaultOneYear(
                param.getTransactionDateStartStr(), param.getTransactionDateEndStr());
        q.setTransactionDateStart(range[0]);
        q.setTransactionDateEnd(range[1]);

        q.setTxnTypes("expense");
        if (!StringTool.isNullOrEmpty(param.getConsumeID())) {
            q.setConsumeID(param.getConsumeID());
        }
        if (!StringTool.isNullOrEmpty(param.getDemoArea())) {
            q.setDemoArea(param.getDemoArea());
        }
        if (!StringTool.isNullOrEmpty(param.getCardTypeName())) {
            q.setCardTypeName(param.getCardTypeName());
        }
        if (!StringTool.isNullOrEmpty(param.getEmptyConsume())) {
            String v = String.valueOf(param.getEmptyConsume()).trim();
            if ("1".equals(v) || "true".equalsIgnoreCase(v)) {
                q.setEmptyConsume(Boolean.TRUE);
            }
        }

        Page page = new Page(param.getPage(), param.getRows());
        CollectionResult<Transaction> result = new CollectionResult<>();
        result.setRows(transactionRepository.getTransactions(q, page));
        result.setTotal(transactionRepository.countTransaction(q));
        return result;
    }
}

