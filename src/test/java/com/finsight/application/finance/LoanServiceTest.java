package com.finsight.application.finance;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.authentication.LedgerUserScope;
import com.finsight.application.card.BankCardService;
import com.finsight.domain.model.BankCard;
import com.finsight.domain.model.Loan;
import com.finsight.domain.port.LoanRepository;
import com.finsight.web.api.dto.LoanWriteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private BankCardService bankCardService;
    @Mock
    private LedgerUserScope ledgerUserScope;
    @Mock
    private AuthenticationFacade authenticationFacade;

    private LoanService service;

    @BeforeEach
    void setUp() {
        service = new LoanService(loanRepository, bankCardService, ledgerUserScope, authenticationFacade);
        when(authenticationFacade.getUserName()).thenReturn("user1");
    }

    @Test
    void listWithSummary_computesWeightedRate() {
        Loan high = loan("l1", "5.86", "5880000", "25725");
        Loan low = loan("l2", "2.80", "126000", "500");
        when(loanRepository.listActive("user1")).thenReturn(List.of(high, low));

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) service.listWithSummary().get("summary");
        assertEquals(2, summary.get("loanCount"));
        assertEquals(new BigDecimal("6006000"), summary.get("totalOutstanding"));
        assertEquals(new BigDecimal("26225"), summary.get("totalMonthlyPayment"));
        assertEquals(5.8, ((Number) summary.get("weightedAvgRatePct")).doubleValue(), 0.05);
    }

    @Test
    void create_requiresDisbursementCard() {
        LoanWriteRequest req = new LoanWriteRequest();
        req.setLenderName("交通银行");
        req.setPrincipalAmount(new BigDecimal("100000"));
        req.setDisbursementCardId("card-1");
        BankCard card = new BankCard();
        card.setId("card-1");
        card.setCreatedBy("user1");
        when(bankCardService.getById("card-1")).thenReturn(card);
        when(loanRepository.save(any(), eq("user1"), eq("user1"))).thenAnswer(inv -> inv.getArgument(0));

        Loan saved = service.create(req);
        assertEquals("card-1", saved.getDisbursementCardId());
    }

    private static Loan loan(String id, String rate, String balance, String monthly) {
        Loan l = new Loan();
        l.setId(id);
        l.setStatus("ACTIVE");
        l.setLenderName("Bank");
        l.setInterestRatePct(new BigDecimal(rate));
        l.setOutstandingBalance(new BigDecimal(balance));
        l.setPrincipalAmount(new BigDecimal(balance));
        l.setMonthlyPayment(new BigDecimal(monthly));
        return l;
    }
}
