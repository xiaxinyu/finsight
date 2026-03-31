package com.finsight.web.contoller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AnalysisController {
    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

    @RequestMapping("/analysis/month-income-expense.html")
    public String monthIncomeExpense(ModelMap model) {
        log.info("************ Open: Income vs Expense Monthly Trend ************");
        return "analysis/month_income_expense_trend";
    }

    @RequestMapping("/analysis/income-curve.html")
    public String incomeCurve(ModelMap model) {
        log.info("************ Open: Income Curve Report ************");
        return "analysis/income_curve_report";
    }
}
