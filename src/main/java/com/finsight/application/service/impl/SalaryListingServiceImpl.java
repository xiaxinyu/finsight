package com.finsight.application.service.impl;

import com.finsight.application.service.ISalaryListingService;
import com.finsight.application.service.ITransactionService;
import com.finsight.application.service.support.ListingDateSupport;
import com.finsight.core.AppServiceException;
import com.finsight.core.StringTool;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.TransactionParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Builds payroll (salary) transaction query and returns paged {@link CollectionResult}.
 */
@Service
public class SalaryListingServiceImpl implements ISalaryListingService {

    /** Consume code for salary / payroll income in this product. */
    public static final String SALARY_INCOME_CONSUME_CODE = "INC-01";

    @Autowired
    private ITransactionService transactionService;

    @Override
    public CollectionResult<Transaction> listSalaryTransactions(TransactionParam param) throws AppServiceException {
        Transaction transaction = new Transaction();
        if (!StringTool.isNullOrEmpty(param.getTransactionDateStartStr())) {
            transaction.setTransactionDateStart(
                    ListingDateSupport.parseMmDdYyyy(param.getTransactionDateStartStr()));
        }
        if (!StringTool.isNullOrEmpty(param.getTransactionDateEndStr())) {
            transaction.setTransactionDateEnd(
                    ListingDateSupport.parseMmDdYyyy(param.getTransactionDateEndStr()));
        }
        transaction.setConsumeCode(SALARY_INCOME_CONSUME_CODE);
        if (!StringTool.isNullOrEmpty(param.getDemoArea())) {
            transaction.setDemoArea(param.getDemoArea());
        }

        Page page = new Page(param.getPage(), param.getRows());
        CollectionResult<Transaction> result = new CollectionResult<>();
        result.setRows(transactionService.getTransactions(transaction, page));
        result.setTotal(transactionService.countTransaction(transaction));
        return result;
    }
}
