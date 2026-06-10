package com.finsight.application.transaction;

import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.Transaction;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.TransactionParam;

/**
 * Maps {@link TransactionParam} to query model and loads paged transactions for UI grids.
 */
public interface ITransactionListingService {

    CollectionResult<Transaction> listTransactions(TransactionParam param) throws AppServiceException;
}
