package com.finsight.web.api.report;

import com.finsight.application.report.TransactionReportFacade;
import com.finsight.web.api.support.ControllerHelper;
import com.finsight.web.api.dto.CommonResult;
import com.finsight.web.api.dto.TransactionParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TransactionReportController extends ControllerHelper {
	private static final Logger logger = LoggerFactory.getLogger(TransactionReportController.class);

	@Autowired
	private TransactionReportFacade transactionReportFacade;

	@RequestMapping("/transaction-report/consume")
	@ResponseBody
	public CommonResult consumeReport(TransactionParam param){
		return runCommon(logger, "consume report", () ->
				CommonResult.success(transactionReportFacade.consumeReportJson(param)));
	}

	@RequestMapping("/transaction-report/week-consume")
	@ResponseBody
	public CommonResult weekConsumeReport(TransactionParam param){
		return runCommon(logger, "week consume report", () ->
				CommonResult.success(transactionReportFacade.weekConsumeReportJson(param)));
	}

	@RequestMapping("/transaction-report/month-consume")
	@ResponseBody
	public CommonResult monthConsumeReport(TransactionParam param){
		return runCommon(logger, "month consume report", () ->
				CommonResult.success(transactionReportFacade.monthConsumeReportJson(param)));
	}

	@RequestMapping("/transaction-report/month-income")
	@ResponseBody
	public CommonResult monthIncomeReport(TransactionParam param){
		return runCommon(logger, "month income report", () ->
				CommonResult.success(transactionReportFacade.monthIncomeReportJson(param)));
	}

	@RequestMapping("/transaction-report/month-expense")
	@ResponseBody
	public CommonResult monthExpenseReport(TransactionParam param){
		return runCommon(logger, "month expense report", () ->
				CommonResult.success(transactionReportFacade.monthExpenseReportJson(param)));
	}

	/** @deprecated Dashboard uses {@code GET /api/v1/analytics/metrics/period-summary}; scheduled for removal. */
	@Deprecated(since = "2.0.2", forRemoval = true)
	@RequestMapping("/transaction-report/home-summary")
	@ResponseBody
	public CommonResult homeSummary(String year, TransactionParam param){
		return runCommon(logger, "home summary", () ->
				CommonResult.success(transactionReportFacade.homeSummary(
						year,
						param == null ? null : param.getTransactionDateStartStr(),
						param == null ? null : param.getTransactionDateEndStr())));
	}
}
