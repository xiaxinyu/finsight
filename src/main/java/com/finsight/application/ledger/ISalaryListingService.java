package com.finsight.application.ledger;

import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.Transaction;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.TransactionParam;

/**
 * Income-related transaction listing for Income Management.
 */
public interface ISalaryListingService {

    CollectionResult<Transaction> listSalaryTransactions(TransactionParam param) throws AppServiceException;
}
