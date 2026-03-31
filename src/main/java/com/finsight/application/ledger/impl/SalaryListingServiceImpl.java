package com.finsight.application.ledger.impl;

import com.finsight.application.ledger.ISalaryListingService;
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
 * Builds income transaction query and returns paged {@link CollectionResult}.
 * <p>
 * Historically this page only queried payroll category (INC-01). We now query by
 * {@code txnTypes=income} so it includes all income categories configured in the
 * consume category tree (e.g. salary, deposit increase, bank loan, etc.).
 */
@Service
public class SalaryListingServiceImpl implements ISalaryListingService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public CollectionResult<Transaction> listSalaryTransactions(TransactionParam param) throws AppServiceException {
        TransactionQuery q = new TransactionQuery();
        java.util.Date[] range = ListingDateSupport.parseMmDdYyyyOrDefaultOneYear(
                param.getTransactionDateStartStr(), param.getTransactionDateEndStr());
        q.setTransactionDateStart(range[0]);
        q.setTransactionDateEnd(range[1]);
        // Query all categories tagged as income instead of only INC-01.
        q.setTxnTypes("income");
        if (!StringTool.isNullOrEmpty(param.getConsumeID())) {
            q.setConsumeID(param.getConsumeID());
        }
        if (!StringTool.isNullOrEmpty(param.getDemoArea())) {
            q.setDemoArea(param.getDemoArea());
        }

        Page page = new Page(param.getPage(), param.getRows());
        CollectionResult<Transaction> result = new CollectionResult<>();
        result.setRows(transactionRepository.getTransactions(q, page));
        result.setTotal(transactionRepository.countTransaction(q));
        return result;
    }
}
