package com.finsight.web.api.expense;

import com.finsight.application.ledger.IExpenseListingService;
import com.finsight.domain.model.Transaction;
import com.finsight.web.api.support.ControllerHelper;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.TransactionParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/expense")
public class ExpenseController extends ControllerHelper {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);

    @Autowired
    private IExpenseListingService expenseListingService;

    @RequestMapping("/getExpenses")
    @ResponseBody
    public CollectionResult<Transaction> getExpenses(TransactionParam param) {
        return runCollection(logger, "get expenses", () -> expenseListingService.listExpenseTransactions(param));
    }
}

