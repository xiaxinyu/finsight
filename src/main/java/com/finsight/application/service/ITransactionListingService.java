package com.finsight.application.service;

import com.finsight.core.AppServiceException;
import com.finsight.domain.model.Transaction;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.TransactionParam;

/**
 * Maps {@link TransactionParam} to query model and loads paged transactions for UI grids.
 */
public interface ITransactionListingService {

    CollectionResult<Transaction> listTransactions(TransactionParam param) throws AppServiceException;
}
