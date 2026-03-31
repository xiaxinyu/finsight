package com.finsight.application.ledger;

import com.finsight.core.AppServiceException;
import com.finsight.domain.model.Transaction;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.TransactionParam;

/**
 * Expense ledger listing (txnTypes=expense) with paging.
 */
public interface IExpenseListingService {
    CollectionResult<Transaction> listExpenseTransactions(TransactionParam param) throws AppServiceException;
}

