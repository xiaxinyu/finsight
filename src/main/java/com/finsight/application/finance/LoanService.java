package com.finsight.application.finance;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.authentication.LedgerUserScope;
import com.finsight.application.card.BankCardService;
import com.finsight.domain.model.BankCard;
import com.finsight.domain.model.Loan;
import com.finsight.domain.model.LoanTxnLink;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.LoanRepository;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.web.api.dto.LoanWriteRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LoanService {

    private static final Set<String> REPAYMENT_METHODS = Set.of(
            "EQUAL_INSTALLMENT", "EQUAL_PRINCIPAL", "INTEREST_FIRST", "BULLET", "OTHER");
    private static final Set<String> LINK_TYPES = Set.of(
            "DISBURSEMENT", "REPAYMENT", "INTEREST", "OTHER");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "CLOSED");

    private final LoanRepository loanRepository;
    private final BankCardService bankCardService;
    private final TransactionRepository transactionRepository;
    private final LedgerUserScope ledgerUserScope;
    private final AuthenticationFacade authenticationFacade;

    public LoanService(LoanRepository loanRepository,
                       BankCardService bankCardService,
                       TransactionRepository transactionRepository,
                       LedgerUserScope ledgerUserScope,
                       AuthenticationFacade authenticationFacade) {
        this.loanRepository = loanRepository;
        this.bankCardService = bankCardService;
        this.transactionRepository = transactionRepository;
        this.ledgerUserScope = ledgerUserScope;
        this.authenticationFacade = authenticationFacade;
    }

    public Map<String, Object> listWithSummary() {
        String userId = userKey();
        List<Loan> loans = loanRepository.listActive(userId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("loans", loans);
        out.put("summary", buildSummary(loans));
        return out;
    }

    public Loan get(String id) {
        return loanRepository.findById(id, userKey())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
    }

    @Transactional
    public Loan create(LoanWriteRequest req) {
        Loan loan = fromRequest(new Loan(), req);
        return loanRepository.save(loan, userKey(), actor());
    }

    @Transactional
    public Loan update(String id, LoanWriteRequest req) {
        Loan existing = get(id);
        Loan loan = fromRequest(existing, req);
        loan.setId(id);
        return loanRepository.save(loan, userKey(), actor());
    }

    @Transactional
    public void delete(String id) {
        get(id);
        loanRepository.softDelete(id, userKey(), actor());
    }

    public List<LoanTxnLink> listLinks(String loanId) {
        get(loanId);
        return loanRepository.listLinks(loanId, userKey());
    }

    @Transactional
    public LoanTxnLink linkTransaction(String loanId, String transactionId, String linkType) {
        Loan loan = get(loanId);
        String type = normalizeLinkType(linkType);
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }
        String txnId = transactionId.trim();
        String userId = userKey();
        if (loanRepository.findLink(loanId, txnId, userId).isPresent()) {
            throw new IllegalArgumentException("This transaction is already linked to the loan");
        }
        Transaction tx = requireOwnedTransaction(txnId);
        assertTransactionOnLoanCard(loan, tx, type);
        LoanTxnLink link = new LoanTxnLink();
        link.setLoanId(loanId);
        link.setTransactionId(txnId);
        link.setLinkType(type);
        return loanRepository.addLink(link, userId, actor());
    }

    @Transactional
    public void unlinkTransaction(String loanId, String transactionId) {
        get(loanId);
        loanRepository.removeLink(loanId, transactionId, userKey());
    }

    private Loan fromRequest(Loan loan, LoanWriteRequest req) {
        if (req.getLenderName() == null || req.getLenderName().isBlank()) {
            throw new IllegalArgumentException("Lender bank is required");
        }
        if (req.getPrincipalAmount() == null || req.getPrincipalAmount().signum() <= 0) {
            throw new IllegalArgumentException("Principal amount must be positive");
        }
        loan.setLenderName(req.getLenderName().trim());
        loan.setLenderBankCode(trimOrNull(req.getLenderBankCode()));
        loan.setName(trimOrNull(req.getName()));
        loan.setPrincipalAmount(req.getPrincipalAmount());
        BigDecimal outstanding = req.getOutstandingBalance() != null
                ? req.getOutstandingBalance()
                : req.getPrincipalAmount();
        if (outstanding.signum() < 0) {
            throw new IllegalArgumentException("Outstanding balance cannot be negative");
        }
        if (outstanding.compareTo(req.getPrincipalAmount()) > 0) {
            throw new IllegalArgumentException("Outstanding balance cannot exceed principal");
        }
        loan.setOutstandingBalance(outstanding);
        loan.setInterestRatePct(req.getInterestRatePct());
        loan.setMonthlyPayment(req.getMonthlyPayment());
        loan.setTermMonths(req.getTermMonths());
        loan.setPaidInstallments(null);
        validateTermMonths(req.getTermMonths());
        loan.setRepaymentMethod(normalizeRepaymentMethod(req.getRepaymentMethod()));
        loan.setMaturityDate(parseDate(req.getMaturityDate()));
        loan.setDisbursementCardId(requireOwnedCard(req.getDisbursementCardId(), "Disbursement card"));
        loan.setRepaymentCardId(optionalOwnedCard(req.getRepaymentCardId()));
        loan.setStatus(normalizeStatus(req.getStatus()));
        loan.setNotes(trimOrNull(req.getNotes()));
        loan.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        return loan;
    }

    private static void validateTermMonths(Integer termMonths) {
        if (termMonths != null && termMonths <= 0) {
            throw new IllegalArgumentException("Term months must be positive");
        }
    }

    private String requireOwnedCard(String cardId, String label) {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException(label + " is required — select the card that receives loan proceeds");
        }
        assertOwnedCard(cardId.trim());
        return cardId.trim();
    }

    private String optionalOwnedCard(String cardId) {
        if (cardId == null || cardId.isBlank()) {
            return null;
        }
        assertOwnedCard(cardId.trim());
        return cardId.trim();
    }

    private void assertOwnedCard(String cardId) {
        BankCard card = bankCardService.getById(cardId);
        if (card == null || (card.getDeleted() != null && card.getDeleted() == 1)) {
            throw new IllegalArgumentException("Bank card not found: " + cardId);
        }
        ledgerUserScope.assertOwned(card.getCreatedBy());
    }

    private Transaction requireOwnedTransaction(String transactionId) {
        Transaction tx = transactionRepository.selectById(transactionId);
        if (tx == null || (tx.getDeleted() != null && tx.getDeleted() == 1)) {
            throw new IllegalArgumentException("Transaction not found");
        }
        ledgerUserScope.assertOwned(tx.getCreatedBy());
        return tx;
    }

    private void assertTransactionOnLoanCard(Loan loan, Transaction tx, String linkType) {
        String cardId = tx.getBankCardId() != null ? tx.getBankCardId() : tx.getCardId();
        if (cardId == null || cardId.isBlank()) {
            return;
        }
        String disbursement = loan.getDisbursementCardId();
        String repayment = loan.getRepaymentCardId() != null ? loan.getRepaymentCardId() : disbursement;
        boolean onDisbursement = disbursement != null && disbursement.equals(cardId);
        boolean onRepayment = repayment != null && repayment.equals(cardId);
        if ("DISBURSEMENT".equals(linkType) && !onDisbursement) {
            throw new IllegalArgumentException("Disbursement links must use the loan disbursement card");
        }
        if (("REPAYMENT".equals(linkType) || "INTEREST".equals(linkType)) && !onRepayment && !onDisbursement) {
            throw new IllegalArgumentException("Repayment links must use the loan repayment or disbursement card");
        }
    }

    private static Map<String, Object> buildSummary(List<Loan> loans) {
        List<Loan> active = loans.stream()
                .filter(l -> !"CLOSED".equalsIgnoreCase(l.getStatus()))
                .toList();
        BigDecimal totalPrincipal = sum(active, Loan::getPrincipalAmount);
        BigDecimal totalOutstanding = sum(active, l ->
                l.getOutstandingBalance() != null ? l.getOutstandingBalance() : l.getPrincipalAmount());
        BigDecimal totalMonthly = sum(active, Loan::getMonthlyPayment);
        double weightedRate = weightedAvgRate(active);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("loanCount", active.size());
        summary.put("totalPrincipal", totalPrincipal);
        summary.put("totalOutstanding", totalOutstanding);
        summary.put("totalMonthlyPayment", totalMonthly);
        summary.put("weightedAvgRatePct", round2(weightedRate));
        return summary;
    }

    private static double weightedAvgRate(List<Loan> loans) {
        double weighted = 0;
        double weightSum = 0;
        for (Loan loan : loans) {
            if (loan.getInterestRatePct() == null) {
                continue;
            }
            double bal = balanceForWeight(loan);
            if (bal <= 0) {
                continue;
            }
            weighted += loan.getInterestRatePct().doubleValue() * bal;
            weightSum += bal;
        }
        return weightSum > 0 ? weighted / weightSum : 0;
    }

    private static double balanceForWeight(Loan loan) {
        if (loan.getOutstandingBalance() != null) {
            return loan.getOutstandingBalance().doubleValue();
        }
        return loan.getPrincipalAmount() != null ? loan.getPrincipalAmount().doubleValue() : 0;
    }

    private static BigDecimal sum(List<Loan> loans, java.util.function.Function<Loan, BigDecimal> fn) {
        BigDecimal total = BigDecimal.ZERO;
        for (Loan loan : loans) {
            BigDecimal v = fn.apply(loan);
            if (v != null) {
                total = total.add(v);
            }
        }
        return total;
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String normalizeRepaymentMethod(String method) {
        if (method == null || method.isBlank()) {
            return null;
        }
        String m = method.trim().toUpperCase();
        if (!REPAYMENT_METHODS.contains(m)) {
            throw new IllegalArgumentException("Invalid repayment method: " + method);
        }
        return m;
    }

    private static String normalizeLinkType(String type) {
        if (type == null || type.isBlank()) {
            return "REPAYMENT";
        }
        String t = type.trim().toUpperCase();
        if (!LINK_TYPES.contains(t)) {
            throw new IllegalArgumentException("Invalid link type: " + type);
        }
        return t;
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        String s = status.trim().toUpperCase();
        if (!STATUSES.contains(s)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        return s;
    }

    private static java.util.Date parseDate(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        return Date.valueOf(LocalDate.parse(iso.trim()));
    }

    private static String trimOrNull(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        return v.trim();
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }

    private String actor() {
        return userKey();
    }
}
