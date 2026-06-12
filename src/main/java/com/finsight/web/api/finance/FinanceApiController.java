package com.finsight.web.api.finance;

import com.finsight.application.finance.BillService;
import com.finsight.application.finance.BudgetService;
import com.finsight.application.finance.CashflowService;
import com.finsight.application.finance.DataQualityService;
import com.finsight.application.finance.FinancialAccountService;
import com.finsight.application.finance.FinancialGoalService;
import com.finsight.application.finance.GoalAdviceService;
import com.finsight.application.finance.FinancialPulseService;
import com.finsight.application.finance.InsightService;
import com.finsight.application.finance.ScenarioService;
import com.finsight.application.finance.TransferService;
import com.finsight.application.finance.WealthService;
import com.finsight.domain.model.Bill;
import com.finsight.domain.model.BudgetLine;
import com.finsight.domain.model.FinancialGoal;
import com.finsight.web.api.dto.CommonResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class FinanceApiController {

    private final FinancialAccountService accountService;
    private final TransferService transferService;
    private final DataQualityService dataQualityService;
    private final FinancialPulseService pulseService;
    private final CashflowService cashflowService;
    private final BudgetService budgetService;
    private final BillService billService;
    private final WealthService wealthService;
    private final FinancialGoalService goalService;
    private final GoalAdviceService goalAdviceService;
    private final ScenarioService scenarioService;
    private final InsightService insightService;

    public FinanceApiController(FinancialAccountService accountService,
                                TransferService transferService,
                                DataQualityService dataQualityService,
                                FinancialPulseService pulseService,
                                CashflowService cashflowService,
                                BudgetService budgetService,
                                BillService billService,
                                WealthService wealthService,
                                FinancialGoalService goalService,
                                GoalAdviceService goalAdviceService,
                                ScenarioService scenarioService,
                                InsightService insightService) {
        this.accountService = accountService;
        this.transferService = transferService;
        this.dataQualityService = dataQualityService;
        this.pulseService = pulseService;
        this.cashflowService = cashflowService;
        this.budgetService = budgetService;
        this.billService = billService;
        this.wealthService = wealthService;
        this.goalService = goalService;
        this.goalAdviceService = goalAdviceService;
        this.scenarioService = scenarioService;
        this.insightService = insightService;
    }

    @GetMapping("/accounts")
    public CommonResult accounts() {
        return CommonResult.success(accountService.listAccounts());
    }

    @GetMapping("/accounts/balances")
    public CommonResult accountBalances() {
        return CommonResult.success(accountService.latestBalances());
    }

    @PostMapping("/accounts/{id}/snapshots")
    public CommonResult recordSnapshot(@PathVariable String id,
                                       @RequestBody Map<String, Object> body) {
        BigDecimal balance = new BigDecimal(String.valueOf(body.get("balance")));
        Date date = body.get("date") != null ? new Date(Long.parseLong(String.valueOf(body.get("date")))) : new Date();
        String source = body.get("source") != null ? String.valueOf(body.get("source")) : "manual";
        return CommonResult.success(accountService.recordSnapshot(id, date, balance, source));
    }

    @GetMapping("/transfers")
    public CommonResult transfers() {
        return CommonResult.success(transferService.listTransfers());
    }

    @PostMapping("/transfers")
    public CommonResult createTransfer(@RequestBody Map<String, String> body) {
        return CommonResult.success(transferService.createTransfer(
                body.get("fromTransactionId"),
                body.get("toTransactionId"),
                body.get("memo")));
    }

    @GetMapping("/data-quality")
    public CommonResult dataQuality() {
        return CommonResult.success(dataQualityService.summary());
    }

    @GetMapping("/financial-pulse")
    public CommonResult pulse() {
        return CommonResult.success(pulseService.pulse());
    }

    @GetMapping("/cashflow")
    public CommonResult cashflow() {
        return CommonResult.success(cashflowService.metrics());
    }

    @GetMapping("/budgets/current")
    public CommonResult currentBudget() {
        return CommonResult.success(budgetService.currentMonthlyBudget());
    }

    @GetMapping("/budgets/current/lines")
    public CommonResult budgetLines() {
        var b = budgetService.currentMonthlyBudget();
        return CommonResult.success(budgetService.linesForBudget(b.getId()));
    }

    @PostMapping("/budgets/lines")
    public CommonResult saveBudgetLine(@RequestBody BudgetLine line) {
        if (line.getBudgetId() == null) {
            line.setBudgetId(budgetService.currentMonthlyBudget().getId());
        }
        return CommonResult.success(budgetService.saveLine(line));
    }

    @GetMapping("/budgets/vs-actual")
    public CommonResult budgetVsActual(
            @RequestParam(value = "transactionDateStartStr", required = false) String startStr,
            @RequestParam(value = "transactionDateEndStr", required = false) String endStr) throws com.finsight.common.exception.AppServiceException {
        return CommonResult.success(budgetService.budgetVsActual(startStr, endStr));
    }

    @GetMapping("/bills")
    public CommonResult bills() {
        return CommonResult.success(billService.listEnabled());
    }

    @PostMapping("/bills")
    public CommonResult saveBill(@RequestBody Bill bill) {
        return CommonResult.success(billService.save(bill));
    }

    @GetMapping("/bills/calendar")
    public CommonResult billCalendar() {
        return CommonResult.success(billService.calendarNext30Days());
    }

    @GetMapping("/wealth")
    public CommonResult wealth() {
        return CommonResult.success(wealthService.snapshot());
    }

    @GetMapping("/goals")
    public CommonResult goals() {
        return CommonResult.success(goalService.list());
    }

    @PostMapping("/goals")
    public CommonResult saveGoal(@RequestBody FinancialGoal goal) {
        return CommonResult.success(goalService.save(goal));
    }

    @GetMapping("/goals/{id}/progress")
    public CommonResult goalProgress(@PathVariable String id) {
        FinancialGoal g = goalService.list().stream().filter(x -> id.equals(x.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        return CommonResult.success(goalService.progress(g));
    }

    @GetMapping("/goals/{id}/advice")
    public CommonResult goalAdvice(@PathVariable String id) throws Exception {
        return CommonResult.success(goalAdviceService.advise(id));
    }

    @PostMapping("/scenarios/simulate")
    public CommonResult simulate(@RequestBody Map<String, Object> body) {
        double lump = num(body.get("lumpSumExpense"));
        double incomePct = num(body.get("incomeChangePct"));
        double bill = num(body.get("newMonthlyBill"));
        return CommonResult.success(scenarioService.simulate(lump, incomePct, bill));
    }

    @GetMapping("/insights/decision-cards")
    public CommonResult decisionCards() throws com.finsight.common.exception.AppServiceException {
        return CommonResult.success(insightService.decisionCards());
    }

    private static double num(Object v) {
        if (v == null) {
            return 0;
        }
        return Double.parseDouble(String.valueOf(v));
    }
}
