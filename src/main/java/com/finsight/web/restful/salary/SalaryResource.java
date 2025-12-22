package com.finsight.web.restful.salary;

import com.finsight.web.restful.common.ControllerHelper;
import com.finsight.application.service.ITransactionService;
import com.finsight.core.DateTool;
import com.finsight.core.StringTool;
import com.finsight.domain.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.finsight.domain.model.Page;
import com.finsight.core.AppServiceException;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.TransactionParam;

@Controller
@RequestMapping("/salary")
public class SalaryResource extends ControllerHelper {
	private static final Logger logger = LoggerFactory.getLogger(SalaryResource.class);

	@Autowired
	private ITransactionService transactionService;

	@RequestMapping("/getSalarys")
	@ResponseBody
	public CollectionResult<Transaction> getSalarys(TransactionParam param) {
		try {
			// Fetch params
			Transaction transaction = new Transaction();
			if (!StringTool.isNullOrEmpty(param.getTransactionDateStartStr())) {
				transaction.setTransactionDateStart(DateTool.changeStringToDate(param.getTransactionDateStartStr(), DateTool.DF_MM_DD_YYYY));
			}
			if (!StringTool.isNullOrEmpty(param.getTransactionDateEndStr())) {
				transaction.setTransactionDateEnd(DateTool.changeStringToDate(param.getTransactionDateEndStr(), DateTool.DF_MM_DD_YYYY));
			}
			// Set consumeCode to 'INC-01' for salary/payroll
			transaction.setConsumeCode("INC-01");
			
			if (!StringTool.isNullOrEmpty(param.getDemoArea())) {
				transaction.setDemoArea(param.getDemoArea());
			}

			Page page = new Page(param.getPage(), param.getRows());
			CollectionResult<Transaction> result = new CollectionResult<Transaction>();
			result.setRows(transactionService.getTransactions(transaction, page));
			result.setTotal(transactionService.countTransaction(transaction));
			return result;
		} catch (AppServiceException e) {
			logger.error("get salarys failed. params[message = " + e.getMessage() + "]", e);
		} catch (Exception e) {
			logger.error("get salarys failed. params[message = " + e.getMessage() + "]", e);
		}
		return null;
	}
}
