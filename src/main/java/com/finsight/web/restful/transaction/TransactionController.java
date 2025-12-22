package com.finsight.web.restful.transaction;

import com.finsight.application.authentication.AuthenticationFacade;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.finsight.core.DateParseException;
import com.finsight.core.DateTool;
import com.finsight.core.StringTool;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.model.Page;
import com.finsight.core.AppServiceException;
import com.finsight.application.service.ITransactionService;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.CommonResult;
import com.finsight.web.restful.model.TransactionParam;
import com.finsight.web.restful.model.ResultCode;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.domain.model.ConsumeCategory;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

@Controller
public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    ITransactionService transactionService;

    @Autowired
    AuthenticationFacade authenticationFacade;

    @Autowired
    ClassificationService classificationService;

    @Autowired
    ConsumeCategoryService consumeCategoryService;

    @RequestMapping("/transaction/getTransactions")
    @ResponseBody
    public CollectionResult<Transaction> getTransactions(TransactionParam param) {
        try {
            //Fetch params
            Transaction transaction = new Transaction();
            if (!StringTool.isNullOrEmpty(param.getTransactionDateStartStr())) {
                transaction.setTransactionDateStart(DateTool.changeStringToDate(param.getTransactionDateStartStr(), DateTool.DF_MM_DD_YYYY));
            }
            if (!StringTool.isNullOrEmpty(param.getTransactionDateEndStr())) {
                transaction.setTransactionDateEnd(DateTool.changeStringToDate(param.getTransactionDateEndStr(), DateTool.DF_MM_DD_YYYY));
            }
            if (!StringTool.isNullOrEmpty(param.getConsumptionType())) {
                transaction.setConsumptionType(StringTool.changeObjToInt(StringUtils.trim(param.getConsumptionType())));
            }
            if (!StringTool.isNullOrEmpty(param.getCardTypeName())) {
                transaction.setCardTypeName(StringUtils.trim(param.getCardTypeName()));
            }
            if (!StringTool.isNullOrEmpty(param.getCardId())) {
                transaction.setBankCardId(StringUtils.trim(param.getCardId()));
            }
            if (!StringTool.isNullOrEmpty(param.getConsumeName())) {
                transaction.setConsumeName(StringUtils.trim(param.getConsumeName()));
            }
            if (!StringTool.isNullOrEmpty(param.getConsumeID())) {
                transaction.setConsumes(param.getConsumeID().split(","));
            }
            if (!StringTool.isNullOrEmpty(param.getDemoArea())) {
                transaction.setDemoArea(StringUtils.trim(param.getDemoArea()));
            }
            if (!StringTool.isNullOrEmpty(param.getWeekName())) {
                transaction.setWeekName(StringUtils.trim(param.getWeekName()));
            }
            if (!StringTool.isNullOrEmpty(param.getYear())) {
                transaction.setYear(StringUtils.trim(param.getYear()));
            }
            if (!StringTool.isNullOrEmpty(param.getMonth())) {
                transaction.setMonth(DateTool.getMonthCode(StringUtils.trim(param.getMonth())));
            }
            if (!StringTool.isNullOrEmpty(param.getTxnTypes())) {
                transaction.setTxnTypes(StringUtils.trim(param.getTxnTypes()));
            }
            if (!StringTool.isNullOrEmpty(param.getEmptyConsume())) {
                String v = StringUtils.trim(param.getEmptyConsume());
                if ("1".equals(v) || "true".equalsIgnoreCase(v)) {
                    transaction.setEmptyConsume(Boolean.TRUE);
                }
            }
            Page page = new Page(param.getPage(), param.getRows());

            CollectionResult<Transaction> result = new CollectionResult<Transaction>();
            StopWatch stopWatch = new StopWatch("消费数据查询统计");
            stopWatch.start("查询列表数据");
            result.setRows(transactionService.getTransactions(transaction, page));
            if (result.getRows() != null) {
                for (Transaction t : result.getRows()) {
                    try {
                        String ds = t.getTransactionDate() == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd").format(t.getTransactionDate());
                        String ts = org.apache.commons.lang3.StringUtils.trimToEmpty(t.getTransactionTime());
                        t.setTransactionDateTime(org.apache.commons.lang3.StringUtils.isNotBlank(ts) ? (ds + " " + ts) : ds);
                    } catch (Exception ignore) {}
                }
            }
            stopWatch.stop();

            stopWatch.start("查询统计数据");
            result.setTotal(transactionService.countTransaction(transaction));
            stopWatch.stop();

            String prettyPrint = stopWatch.prettyPrint();
            logger.info("耗时打印：{}", prettyPrint);

            return result;
        } catch (AppServiceException e) {
            logger.error("get transactions failed. params[message = " + e.getMessage() + "]", e);
        } catch (DateParseException e) {
            logger.error("date type's params error. params[message = " + e.getMessage() + "]", e);
        }
        CollectionResult<Transaction> empty = new CollectionResult<>();
        empty.setTotal(0);
        empty.setRows(java.util.Collections.emptyList());
        return empty;
    }

    @RequestMapping("/transaction/delete")
    @ResponseBody
    public CommonResult deleteTransaction(String id) {
        try {
            transactionService.deleteTransaction(id);
            return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), "操作成功.");
        } catch (AppServiceException e) {
            logger.error("delete transaction failed. params[id = " + id + "]", e);
            return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage());
        }
    }

    @RequestMapping("/transaction/update")
    @ResponseBody
    public CommonResult updateTransaction(Transaction transaction) {
        try {
            transactionService.updateTransaction(transaction, authenticationFacade.getUserName());
            return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), "操作成功.");
        } catch (AppServiceException e) {
            logger.error("delete transaction failed. params[id = " + transaction.getId() + ",consumptionType = " + transaction.getConsumptionType() + "]", e);
            return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage());
        }
    }

    @RequestMapping("/transaction/update-batch")
    @ResponseBody
    public CommonResult updateTransactionBatch(@org.springframework.web.bind.annotation.RequestBody String payload) {
        try {
            java.util.List<Transaction> transactions = java.util.Collections.emptyList();
            try {
                transactions = JSON.parseArray(payload, Transaction.class);
            } catch (Exception ex) {
                transactions = java.util.Collections.emptyList();
            }
            if (transactions == null || transactions.isEmpty()) {
                return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), "empty_payload");
            }
            int success = 0;
            int failed = 0;
            for (Transaction c : transactions) {
                try {
                    String id = org.apache.commons.lang3.StringUtils.trimToEmpty(c.getId());
                    String code = org.apache.commons.lang3.StringUtils.trimToEmpty(c.getConsumeCode());
                    String name = org.apache.commons.lang3.StringUtils.trimToEmpty(c.getConsumeName());
                    if (id.isEmpty() || code.isEmpty()) {
                        failed++;
                        continue;
                    }
                    if (name.isEmpty()) {
                        ConsumeCategory cat = consumeCategoryService.getOne(
                                Wrappers.<ConsumeCategory>lambdaQuery()
                                        .eq(ConsumeCategory::getCode, code)
                                        .ne(ConsumeCategory::getDeleted, 1),
                                false
                        );
                        if (cat != null && org.apache.commons.lang3.StringUtils.isNotBlank(cat.getName())) {
                            c.setConsumeName(cat.getName());
                        }
                    }
                    transactionService.updateTransaction(c, authenticationFacade.getUserName());
                    success++;
                } catch (AppServiceException e) {
                    failed++;
                    logger.warn("batch update failed for id={}, error={}", c.getId(), e.getMessage());
                } catch (Exception e) {
                    failed++;
                    logger.warn("batch update failed for id={}, error={}", c.getId(), e.getMessage());
                }
            }
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("success", success);
            result.put("failed", failed);
            return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), com.alibaba.fastjson.JSONObject.toJSONString(result));
        } catch (Exception e) {
            logger.error("batch update transactions failed. params[message = " + e.getMessage() + "]", e);
            return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage());
        }
    }

    @RequestMapping("/transaction/classify")
    @ResponseBody
    public CommonResult classifyTransaction(Transaction transaction, @org.springframework.web.bind.annotation.RequestParam(value = "bankCode", required = false) String bankCode, @org.springframework.web.bind.annotation.RequestParam(value = "cardTypeCode", required = false) String cardTypeCode){
        try{
            String narration = transaction.getTransactionDesc();
            bankCode = org.apache.commons.lang3.StringUtils.trimToEmpty(bankCode);
            cardTypeCode = org.apache.commons.lang3.StringUtils.trimToEmpty(cardTypeCode);
            if(!org.apache.commons.lang3.StringUtils.isNotBlank(bankCode)){
                bankCode = org.apache.commons.lang3.StringUtils.trimToEmpty(transaction.getBankCardName());
            }
            if(!org.apache.commons.lang3.StringUtils.isNotBlank(cardTypeCode)){
                cardTypeCode = org.apache.commons.lang3.StringUtils.trimToEmpty(transaction.getCardTypeName());
            }
            if(org.apache.commons.lang3.StringUtils.isNotBlank(cardTypeCode)){
                cardTypeCode = cardTypeCode.trim().toLowerCase();
            }
            java.util.List<com.finsight.application.consume.ClassificationService.Result> rs = classificationService.classifyTopN(narration, bankCode, cardTypeCode, 5);
            if(rs == null || rs.isEmpty()){
                return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), "no_match");
            }
            String payload = com.alibaba.fastjson.JSON.toJSONString(rs);
            return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), payload);
        }catch(Exception e){
            logger.error("classify transaction failed. params[desc = " + transaction.getTransactionDesc() + "]", e);
            return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage());
        }
    }
 
    @RequestMapping("/transaction/keywords")
    @ResponseBody
    public CommonResult extractKeywords(Transaction transaction){
        try{
            String narration = transaction.getTransactionDesc();
            java.util.List<String> ks = classificationService == null ? java.util.Collections.emptyList() : classificationServiceTokens(narration);
            if(ks == null) ks = java.util.Collections.emptyList();
            String payload = com.alibaba.fastjson.JSON.toJSONString(ks);
            return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), payload);
        }catch(Exception e){
            logger.error("extract keywords failed. params[desc = " + transaction.getTransactionDesc() + "]", e);
            return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage());
        }
    }
 
    private java.util.List<String> classificationServiceTokens(String narration){
        try{
            java.util.List<String> ks = classificationService == null ? null : classificationService.tokens(narration);
            return ks;
        }catch(Exception e){
            return java.util.Collections.emptyList();
        }
    }
}
