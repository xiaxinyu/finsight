package com.finsight.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Legacy Thymeleaf / EasyUI URLs → React SPA redirects.
 */
@Controller
public class LegacyRedirectController {

    // --- Site entry ---

    @RequestMapping({"", "index.html"})
    public String index() {
        return "redirect:/app/dashboard";
    }

    @RequestMapping("north.html")
    public String north() {
        return "redirect:/app/dashboard";
    }

    @RequestMapping("menu.html")
    public String menu() {
        return "redirect:/app/dashboard";
    }

    @RequestMapping("navigation.html")
    public String navigation() {
        return "redirect:/app/dashboard";
    }

    @RequestMapping("login.html")
    public String login() {
        return "redirect:/app/login";
    }

    @RequestMapping("login-error.html")
    public String loginError() {
        return "redirect:/app/login";
    }

    // --- Account / admin ---

    @RequestMapping("account/index.html")
    public String accountIndex() {
        return "redirect:/app/dashboard";
    }

    @RequestMapping("/account/transaction/transaction_bill.html")
    public String transactionBill() {
        return "redirect:/app/transactions";
    }

    @RequestMapping({"/account/salary", "/account/salary/Salary.html"})
    public String salary() {
        return "redirect:/app/ledgers/salary";
    }

    @RequestMapping({"/account/house-rent", "/account/house-rent/HouseRent.html"})
    public String houseRent() {
        return "redirect:/app/ledgers/house-rent";
    }

    @RequestMapping({"/account/expense", "/account/expense/Expense.html"})
    public String expense() {
        return "redirect:/app/ledgers/expense";
    }

    @RequestMapping({"/account/endowment", "/account/endowment/Endowment.html"})
    public String endowment() {
        return "redirect:/app/ledgers/endowment";
    }

    @RequestMapping({"/account/accumulation", "/account/accumulation/accumulation.html"})
    public String accumulation() {
        return "redirect:/app/ledgers/accumulation";
    }

    @RequestMapping({"/account/medical", "/account/medical/Medical.html"})
    public String medical() {
        return "redirect:/app/ledgers/medical";
    }

    @RequestMapping({"/account/unemployment", "/account/unemployment/UnEmployment.html"})
    public String unemployment() {
        return "redirect:/app/ledgers/unemployment";
    }

    @RequestMapping("/system/admin/consume_rules.html")
    public String consumeRules() {
        return "redirect:/app/admin/rules";
    }

    @RequestMapping("/system/admin/consume_categories.html")
    public String consumeCategories() {
        return "redirect:/app/admin/categories";
    }

    @RequestMapping("/system/admin/cards.html")
    public String bankCards() {
        return "redirect:/app/admin/cards";
    }

    @RequestMapping("/system/admin/users.html")
    public String users() {
        return "redirect:/app/admin/users";
    }

    @RequestMapping("/account/statement/upload.html")
    public String accountStatementUpload() {
        return "redirect:/app/statements/upload";
    }

    // --- Reports (account + /reports paths) ---

    @RequestMapping("/account/transaction/report/consume_line_report.html")
    public String consumeLineReport() {
        return "redirect:/app/reports/transaction-trend";
    }

    @RequestMapping("/account/transaction/report/consume_pie_report.html")
    public String consumePieReport() {
        return "redirect:/app/reports/category-breakdown";
    }

    @RequestMapping("/account/transaction/report/consume_compare_report.html")
    public String consumeCompareReport() {
        return "redirect:/app/reports/category-comparison";
    }

    @RequestMapping("/account/transaction/report/month_consume_report.html")
    public String monthConsumeReport() {
        return "redirect:/app/reports/monthly-comparison";
    }

    @RequestMapping("/account/transaction/report/week_consume_report.html")
    public String weekConsumeReport() {
        return "redirect:/app/reports/weekly-summary";
    }

    @GetMapping("/reports/income-vs-expense.html")
    public String reportsIncomeVsExpense() {
        return "redirect:/app/reports/income-vs-expense";
    }

    @GetMapping("/reports/transaction-trend.html")
    public String reportsTransactionTrend() {
        return "redirect:/app/reports/transaction-trend";
    }

    @GetMapping("/reports/category-breakdown.html")
    public String reportsCategoryBreakdown() {
        return "redirect:/app/reports/category-breakdown";
    }

    @GetMapping("/reports/category-comparison.html")
    public String reportsCategoryComparison() {
        return "redirect:/app/reports/category-comparison";
    }

    @GetMapping("/reports/weekly-summary.html")
    public String reportsWeeklySummary() {
        return "redirect:/app/reports/weekly-summary";
    }

    @GetMapping("/reports/monthly-comparison.html")
    public String reportsMonthlyComparison() {
        return "redirect:/app/reports/monthly-comparison";
    }

    @GetMapping("/reports/income-curve.html")
    public String reportsIncomeCurve() {
        return "redirect:/app/reports/income-curve";
    }

    @GetMapping("/reports/expense-curve.html")
    public String reportsExpenseCurve() {
        return "redirect:/app/reports/expense-curve";
    }

    // --- Analysis (legacy aliases) ---

    @RequestMapping("/analysis/month-income-expense.html")
    public String analysisIncomeVsExpense() {
        return "redirect:/app/reports/income-vs-expense";
    }

    @RequestMapping("/analysis/income-curve.html")
    public String analysisIncomeCurve() {
        return "redirect:/app/reports/income-curve";
    }

    @RequestMapping("/analysis/expense-curve.html")
    public String analysisExpenseCurve() {
        return "redirect:/app/reports/expense-curve";
    }

    // --- Statements ---

    @GetMapping("/statement/upload.html")
    public String statementUpload() {
        return "redirect:/app/statements/upload";
    }

    @GetMapping("/statement/list.html")
    public String statementList() {
        return "redirect:/app/statements";
    }
}
