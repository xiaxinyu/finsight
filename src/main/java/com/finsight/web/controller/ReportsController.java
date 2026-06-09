package com.finsight.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reports")
public class ReportsController {

    @GetMapping("/income-vs-expense.html")
    public String incomeVsExpense() {
        return "redirect:/app/reports/income-vs-expense";
    }

    @GetMapping("/transaction-trend.html")
    public String transactionTrend() {
        return "redirect:/app/reports/transaction-trend";
    }

    @GetMapping("/category-breakdown.html")
    public String categoryBreakdown() {
        return "redirect:/app/reports/category-breakdown";
    }

    @GetMapping("/category-comparison.html")
    public String categoryComparison() {
        return "redirect:/app/reports/category-comparison";
    }

    @GetMapping("/weekly-summary.html")
    public String weeklySummary() {
        return "redirect:/app/reports/weekly-summary";
    }

    @GetMapping("/monthly-comparison.html")
    public String monthlyComparison() {
        return "redirect:/app/reports/monthly-comparison";
    }

    @GetMapping("/income-curve.html")
    public String incomeCurve() {
        return "redirect:/app/reports/income-curve";
    }

    @GetMapping("/expense-curve.html")
    public String expenseCurve() {
        return "redirect:/app/reports/expense-curve";
    }
}
