package com.finsight.application.service;

import com.finsight.core.AppServiceException;
import com.finsight.domain.model.Transaction;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.TransactionParam;

/**
 * Salary-related transaction listing (payroll income category INC-01).
 */
public interface ISalaryListingService {

    CollectionResult<Transaction> listSalaryTransactions(TransactionParam param) throws AppServiceException;
}
