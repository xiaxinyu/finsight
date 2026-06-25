package com.finsight.web.api.transaction;

import com.finsight.application.authentication.AuthenticationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.finsight.domain.model.Transaction;
import com.finsight.application.transaction.ITransactionBatchUpdateService;
import com.finsight.application.transaction.ITransactionClassificationService;
import com.finsight.application.transaction.ITransactionListingService;
import com.finsight.application.transaction.ITransactionService;
import com.finsight.application.transaction.TransactionReclassificationResult;
import com.finsight.application.transaction.TransactionReclassificationService;
import com.alibaba.fastjson.JSON;
import com.finsight.application.support.TransactionIdList;
import com.finsight.web.api.support.ControllerHelper;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.CommonResult;
import com.finsight.web.api.dto.TransactionParam;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

@Controller
public class TransactionController extends ControllerHelper {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    ITransactionService transactionService;

    @Autowired
    private ITransactionListingService transactionListingService;

    @Autowired
    private ITransactionBatchUpdateService transactionBatchUpdateService;

    @Autowired
    private ITransactionClassificationService transactionClassificationService;

    @Autowired
    AuthenticationFacade authenticationFacade;

    @Autowired
    private TransactionReclassificationService transactionReclassificationService;

    @RequestMapping("/transaction/getTransactions")
    @ResponseBody
    public CollectionResult<Transaction> getTransactions(TransactionParam param) {
        return runCollection(logger, "get transactions", () -> transactionListingService.listTransactions(param));
    }

    @RequestMapping("/transaction/delete")
    @ResponseBody
    public CommonResult deleteTransaction(String id) {
        return runCommon(logger, "delete transaction", () -> {
            transactionService.deleteTransaction(id, authenticationFacade.getUserName());
            return CommonResult.success(OPERATION_OK);
        });
    }

    @RequestMapping("/transaction/update")
    @ResponseBody
    public CommonResult updateTransaction(Transaction transaction) {
        return runCommon(logger, "update transaction", () -> {
            transactionService.updateTransaction(transaction, authenticationFacade.getUserName());
            return CommonResult.success(OPERATION_OK);
        });
    }

    @RequestMapping("/transaction/income-to-expense")
    @ResponseBody
    public CommonResult incomeToExpense(String ids) {
        List<String> idList = TransactionIdList.parseCommaSeparatedIds(ids);
        if (idList.isEmpty()) {
            return CommonResult.fail("empty_ids");
        }
        return runCommonAll(logger, "income to expense", () ->
                CommonResult.success(String.valueOf(
                        transactionService.incomeToExpense(idList, authenticationFacade.getUserName()))));
    }

    @RequestMapping("/transaction/expense-to-income")
    @ResponseBody
    public CommonResult expenseToIncome(String ids) {
        List<String> idList = TransactionIdList.parseCommaSeparatedIds(ids);
        if (idList.isEmpty()) {
            return CommonResult.fail("empty_ids");
        }
        return runCommonAll(logger, "expense to income", () ->
                CommonResult.success(String.valueOf(
                        transactionService.expenseToIncome(idList, authenticationFacade.getUserName()))));
    }

    @RequestMapping("/transaction/update-batch")
    @ResponseBody
    public CommonResult updateTransactionBatch(@RequestBody String payload) {
        return runCommonAll(logger, "batch update transactions", () -> {
            Optional<String> body = transactionBatchUpdateService.batchUpdateTransactions(
                    payload, authenticationFacade.getUserName());
            if (body.isEmpty()) {
                return CommonResult.fail("empty_payload");
            }
            return CommonResult.success(body.get());
        });
    }

    @RequestMapping("/transaction/classify")
    @ResponseBody
    public CommonResult classifyTransaction(
            TransactionParam param,
            Transaction transaction,
            @RequestParam(value = "ids", required = false) String ids,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "persist", required = false, defaultValue = "false") boolean persist,
            @RequestParam(value = "overrideExisting", required = false, defaultValue = "false") boolean overrideExisting,
            @RequestParam(value = "useOtherFallback", required = false, defaultValue = "false") boolean useOtherFallback,
            @RequestParam(value = "bankCode", required = false) String bankCode,
            @RequestParam(value = "cardTypeCode", required = false) String cardTypeCode) {
        return runCommon(logger, "classify transaction", () -> {
            if ("unclassified".equalsIgnoreCase(StringUtils.trimToEmpty(scope))) {
                TransactionReclassificationResult result = transactionReclassificationService.reclassifyUnclassified(
                        param, persist, useOtherFallback, authenticationFacade.getUserName());
                return CommonResult.success(JSON.toJSONString(result));
            }
            if (StringUtils.isNotBlank(ids)) {
                TransactionReclassificationResult result = transactionReclassificationService.reclassify(
                        ids, persist, overrideExisting, useOtherFallback, authenticationFacade.getUserName());
                return CommonResult.success(JSON.toJSONString(result));
            }
            Optional<String> json = transactionClassificationService.suggestTopN(transaction, bankCode, cardTypeCode);
            return json.map(CommonResult::success).orElse(CommonResult.fail("no_match"));
        });
    }

    @RequestMapping("/transaction/keywords")
    @ResponseBody
    public CommonResult extractKeywords(Transaction transaction) {
        return runCommon(logger, "extract keywords", () ->
                CommonResult.success(transactionClassificationService.keywordsJson(transaction)));
    }
}
