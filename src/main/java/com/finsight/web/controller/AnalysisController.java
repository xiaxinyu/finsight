package com.finsight.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AnalysisController {
    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

    @RequestMapping("/analysis/month-income-expense.html")
    public String monthIncomeExpense() {
        log.info("Redirect: Income vs Expense -> Report Insight Shell");
        return "redirect:/app/reports/income-vs-expense";
    }

    @RequestMapping("/analysis/income-curve.html")
    public String incomeCurve() {
        log.info("Redirect: Income Curve -> Report Insight Shell");
        return "redirect:/app/reports/income-curve";
    }

    @RequestMapping("/analysis/expense-curve.html")
    public String expenseCurve() {
        log.info("Redirect: Expense Curve -> Report Insight Shell");
        return "redirect:/app/reports/expense-curve";
    }
}
