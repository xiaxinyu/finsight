package com.finsight.web.restful.report;

import com.finsight.application.service.ITransactionService;
import com.finsight.domain.model.Transaction;
import com.finsight.web.restful.common.ControllerHelper;
import com.finsight.web.restful.model.CommonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;

@Controller
public class TransactionReportController extends ControllerHelper {
	private static final Logger logger = LoggerFactory.getLogger(TransactionReportController.class);
	@Autowired
	private ITransactionService transactionService;
	
	@RequestMapping("/transaction-report/consume")
	@ResponseBody
	public CommonResult consumeReport(Transaction transaction){
		return runCommon(logger, "consume report", () ->
				CommonResult.success(transactionService.consumeReport(transaction)));
	}
	
	@RequestMapping("/transaction-report/week-consume")
	@ResponseBody
	public CommonResult weekConsumeReport(Transaction transaction){
		return runCommon(logger, "week consume report", () ->
				CommonResult.success(transactionService.weekConsumeReport(transaction)));
	}
	
	@RequestMapping("/transaction-report/month-consume")
	@ResponseBody
	public CommonResult monthConsumeReport(Transaction transaction){
		return runCommon(logger, "month consume report", () ->
				CommonResult.success(transactionService.monthConsumeReport(transaction)));
	}

	@RequestMapping("/transaction-report/home-summary")
	@ResponseBody
	public CommonResult homeSummary(String year){
		Integer y;
		if (year == null || year.trim().isEmpty()) {
			y = LocalDate.now().getYear();
		} else {
			try {
				y = Integer.parseInt(year.trim());
			} catch (NumberFormatException ex) {
				return CommonResult.fail("year must be a valid integer");
			}
		}
		int currentYear = LocalDate.now().getYear();
		if (y < 2000 || y > currentYear + 1) {
			return CommonResult.fail("year out of range");
		}
		final Integer yearParam = y;
		return runCommon(logger, "home summary", () ->
				CommonResult.success(transactionService.homeSummary(yearParam)));
	}
}
