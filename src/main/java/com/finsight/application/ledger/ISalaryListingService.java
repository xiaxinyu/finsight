package com.finsight.application.ledger;

import com.finsight.core.AppServiceException;
import com.finsight.domain.model.Transaction;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.TransactionParam;

/**
 * Income-related transaction listing for Income Management.
 */
public interface ISalaryListingService {

    CollectionResult<Transaction> listSalaryTransactions(TransactionParam param) throws AppServiceException;
}
