package com.finsight.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AccountController {
    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    @RequestMapping("account/index.html")
    public String index(ModelMap model) {
        log.info("************ Hello, Account in Private Account ************");
        return "account/index";
    }

    @RequestMapping("/account/transaction/transaction_bill.html")
    public String transactionBill(ModelMap model) {
        log.info("************ Hello, Transaction Bill in Private Account ************");
        return "account/transaction/transaction_bill";
    }

    @RequestMapping("/account/transaction/report/consume_line_report.html")
    public String consumeLineReport(ModelMap model) {
        log.info("************ Hello, Consume Line Report Private Account ************");
        return "account/transaction/report/consume_line_report";
    }

    @RequestMapping("/account/transaction/report/consume_pie_report.html")
    public String consumePieReport(ModelMap model) {
        log.info("************ Hello, Consume Pie Report Private Account ************");
        return "account/transaction/report/consume_pie_report";
    }

    @RequestMapping("/account/transaction/report/consume_compare_report.html")
    public String consumeCompareReport(ModelMap model) {
        log.info("************ Hello, Consume Compare Report Private Account ************");
        return "account/transaction/report/consume_compare_report";
    }

    @RequestMapping("/account/transaction/report/month_consume_report.html")
    public String monthConsumeReport(ModelMap model) {
        log.info("************ Hello, Month Consume Report Private Account ************");
        return "account/transaction/report/month_consume_report";
    }

    @RequestMapping("/account/transaction/report/week_consume_report.html")
    public String weekConsumeReport(ModelMap model) {
        log.info("************ Hello, Week Consume Report Private Account ************");
        return "account/transaction/report/week_consume_report";
    }


    @RequestMapping("/account/salary/Salary.html")
    public String salaryHtml(ModelMap model) {
        log.info("************ Hello, Salary HTML in Private Account ************");
        return "account/salary/Salary";
    }

    @RequestMapping("/account/salary")
    public String salaryRest(ModelMap model) {
        log.info("************ Hello, Salary REST in Private Account ************");
        return "account/salary/Salary";
    }


    @RequestMapping("/account/house-rent/HouseRent.html")
    public String houseRentHtml(ModelMap model) {
        log.info("************ Hello, HouseRent HTML in Private Account ************");
        return "account/house-rent/HouseRent";
    }

    @RequestMapping("/account/house-rent")
    public String houseRentRest(ModelMap model) {
        log.info("************ Hello, HouseRent REST in Private Account ************");
        return "account/house-rent/HouseRent";
    }

    @RequestMapping("/account/expense/Expense.html")
    public String expenseHtml(ModelMap model) {
        log.info("************ Hello, Expense HTML in Private Account ************");
        return "account/expense/Expense";
    }

    @RequestMapping("/account/expense")
    public String expenseRest(ModelMap model) {
        log.info("************ Hello, Expense REST in Private Account ************");
        return "account/expense/Expense";
    }


    @RequestMapping("/account/endowment/Endowment.html")
    public String endowmentHtml(ModelMap model) {
        log.info("************ Hello, Endowment HTML in Private Account ************");
        return "account/endowment/Endowment";
    }

    @RequestMapping("/account/endowment")
    public String endowmentRest(ModelMap model) {
        log.info("************ Hello, Endowment REST in Private Account ************");
        return "account/endowment/Endowment";
    }


    @RequestMapping("/account/accumulation/accumulation.html")
    public String accumulationHtml(ModelMap model) {
        log.info("************ Hello, Accumulation HTML in Private Account ************");
        return "account/accumulation/accumulation";
    }

    @RequestMapping("/account/accumulation")
    public String accumulationRest(ModelMap model) {
        log.info("************ Hello, Accumulation REST in Private Account ************");
        return "account/accumulation/accumulation";
    }

    @RequestMapping("/account/medical/Medical.html")
    public String medicalHtml(ModelMap model) {
        log.info("************ Hello, Medical HTML in Private Account ************");
        return "account/medical/Medical";
    }

    @RequestMapping("/account/medical")
    public String medicalRest(ModelMap model) {
        log.info("************ Hello, Medical REST in Private Account ************");
        return "account/medical/Medical";
    }


    @RequestMapping("/account/unemployment/UnEmployment.html")
    public String unemploymentHtml(ModelMap model) {
        log.info("************ Hello, UnEmployment HTML in Private Account ************");
        return "account/unemployment/UnEmployment";
    }

    @RequestMapping("/account/unemployment")
    public String unemploymentRest(ModelMap model) {
        log.info("************ Hello, UnEmployment REST in Private Account ************");
        return "account/unemployment/UnEmployment";
    }

    @RequestMapping("/system/admin/consume_rules.html")
    public String consumeRules(ModelMap model) {
        log.info("************ Hello, Category Rules in Private Account ************");
        return "system/admin/consume_rules";
    }

    @RequestMapping("/system/admin/consume_categories.html")
    public String consumeCategories(ModelMap model) {
        log.info("************ Hello, Category Types in Private Account ************");
        return "system/admin/consume_categories";
    }

    @RequestMapping("/system/admin/cards.html")
    public String bankCards(ModelMap model) {
        log.info("************ Hello, Bank Cards in Private Account ************");
        return "system/admin/cards";
    }

    @RequestMapping("/system/admin/users.html")
    public String users(ModelMap model) {
        log.info("************ Hello, Users in Private Account ************");
        return "system/admin/users";
    }

    @RequestMapping("/account/statement/upload.html")
    public String statementUpload(ModelMap model) {
        log.info("************ Hello, Statement Upload in Private Account ************");
        return "account/statement/upload";
    }
}
