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
 * Builds income transaction query and returns paged {@link CollectionResult}.
 * <p>
 * Historically this page only queried payroll category (INC-01). We now query by
 * {@code txnTypes=income} so it includes all income categories configured in the
 * consume category tree (e.g. salary, deposit increase, bank loan, etc.).
 */
@Service
public class SalaryListingServiceImpl implements ISalaryListingService {

    @Autowired
    private ITransactionService transactionService;

    @Override
    public CollectionResult<Transaction> listSalaryTransactions(TransactionParam param) throws AppServiceException {
        Transaction transaction = new Transaction();
        java.util.Date[] range = ListingDateSupport.parseMmDdYyyyOrDefaultOneYear(
                param.getTransactionDateStartStr(), param.getTransactionDateEndStr());
        transaction.setTransactionDateStart(range[0]);
        transaction.setTransactionDateEnd(range[1]);
        // Query all categories tagged as income instead of only INC-01.
        transaction.setTxnTypes("income");
        if (!StringTool.isNullOrEmpty(param.getConsumeID())) {
            transaction.setConsumeID(param.getConsumeID());
        }
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
