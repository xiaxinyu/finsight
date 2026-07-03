package com.finsight.application.card;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.authentication.LedgerUserScope;
import com.finsight.common.exception.AppException;
import com.finsight.domain.model.BankCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardFacadeTest {

    @Mock
    private BankCardService bankCardService;

    @Mock
    private AuthenticationFacade authenticationFacade;

    private final LedgerUserScope ledgerUserScope = new LedgerUserScope();
    private CardFacade cardFacade;

    @BeforeEach
    void setUp() {
        cardFacade = new CardFacade();
        ReflectionTestUtils.setField(cardFacade, "bankCardService", bankCardService);
        ReflectionTestUtils.setField(cardFacade, "authenticationFacade", authenticationFacade);
        ReflectionTestUtils.setField(cardFacade, "ledgerUserScope", ledgerUserScope);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("xiaxinyu", "n/a", List.of()));
    }

    @Test
    void update_rejectsForeignCard() {
        BankCard existing = new BankCard();
        existing.setId("id-ccb-d-001");
        existing.setCreatedBy("other-user");
        when(bankCardService.getById("id-ccb-d-001")).thenReturn(existing);

        BankCard patch = new BankCard();
        patch.setCardName("Hacked");

        assertThrows(AppException.class, () -> cardFacade.update("id-ccb-d-001", patch));
    }
}
