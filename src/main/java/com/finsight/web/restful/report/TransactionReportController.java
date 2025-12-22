package com.finsight.web.restful.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.finsight.domain.model.Transaction;
import com.finsight.web.restful.model.CommonResult;
import com.finsight.web.restful.model.ResultCode;
import com.finsight.core.AppServiceException;
import com.finsight.application.service.ITransactionService;

@Controller
public class TransactionReportController {
	private static final Logger logger = LoggerFactory.getLogger(TransactionReportController.class);
	@Autowired
	private ITransactionService transactionService;
	
	@RequestMapping("/transaction-report/consume")
	@ResponseBody
	public CommonResult consumeReport(Transaction transaction){
		try {
			String result = transactionService.consumeReport(transaction);
			return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), result);
		} catch (AppServiceException e) {
			logger.error("delete transaction failed. params[id = " + transaction.getId() + ",consumptionType = " + transaction.getConsumptionType() + "]", e);
			return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage());
		}
	}
	
	@RequestMapping("/transaction-report/week-consume")
	@ResponseBody
	public CommonResult weekConsumeReport(Transaction transaction){
		try {
			String result = transactionService.weekConsumeReport(transaction);
			return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), result);
		} catch (AppServiceException e) {
			logger.error("delete transaction failed. params[id = " + transaction.getId() + ",consumptionType = " + transaction.getConsumptionType() + "]", e);
			return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage());
		}
	}
	
	@RequestMapping("/transaction-report/month-consume")
	@ResponseBody
	public CommonResult monthConsumeReport(Transaction transaction){
		try {
			String result = transactionService.monthConsumeReport(transaction);
			return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), result);
		} catch (AppServiceException e) {
			logger.error("delete transaction failed. params[id = " + transaction.getId() + ",consumptionType = " + transaction.getConsumptionType() + "]", e);
			return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage());
		}
	}

	@RequestMapping("/transaction-report/home-summary")
	@ResponseBody
	public CommonResult homeSummary(String year){
		try{
			Integer y = null;
			if (year != null && year.trim().length() > 0){
				try { y = Integer.parseInt(year.trim()); } catch (Exception ignore){}
			}
			String result = transactionService.homeSummary(y);
			return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), result);
		} catch (AppServiceException e){
			logger.error("home summary failed. params[year = " + year + "]", e);
			return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage());
		}
	}
}
