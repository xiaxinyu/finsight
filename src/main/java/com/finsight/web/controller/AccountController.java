package com.finsight.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Legacy Thymeleaf routes → React SPA redirects.
 */
@Controller
public class AccountController {

    @RequestMapping("account/index.html")
    public String index() {
        return "redirect:/app/dashboard";
    }

    @RequestMapping("/account/transaction/transaction_bill.html")
    public String transactionBill() {
        return "redirect:/app/transactions";
    }

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
    public String statementUpload() {
        return "redirect:/app/statements/upload";
    }
}
