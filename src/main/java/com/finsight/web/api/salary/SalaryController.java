package com.finsight.web.api.salary;

import com.finsight.application.ledger.ISalaryListingService;
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
@RequestMapping("/salary")
public class SalaryController extends ControllerHelper {

    private static final Logger logger = LoggerFactory.getLogger(SalaryController.class);

    @Autowired
    private ISalaryListingService salaryListingService;

    @RequestMapping("/getSalarys")
    @ResponseBody
    public CollectionResult<Transaction> getSalarys(TransactionParam param) {
        return runCollection(logger, "get salarys", () -> salaryListingService.listSalaryTransactions(param));
    }
}
