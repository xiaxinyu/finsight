package com.finsight.application.ledger;

import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.Transaction;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.TransactionParam;

/**
 * Expense ledger listing (txnTypes=expense) with paging.
 */
public interface IExpenseListingService {
    CollectionResult<Transaction> listExpenseTransactions(TransactionParam param) throws AppServiceException;
}

