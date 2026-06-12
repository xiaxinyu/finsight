package com.finsight.application.statement;

import com.finsight.application.card.BankCardService;
import com.finsight.application.consume.ClassificationProperties;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.statement.impl.StatementProcessingServiceImpl;
import com.finsight.domain.model.BankCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementProcessingServiceImplTest {

    @Mock
    private BankCardService bankCardService;

    @Mock
    private ClassificationService classificationService;

    @Mock
    private ClassificationProperties classificationProperties;

    @InjectMocks
    private StatementProcessingServiceImpl service;

    @Test
    void parseAndEnrichTransactions_bindsSingleMatchingCardWhenNoCardNo() {
        BankCard cmbDebit = new BankCard();
        cmbDebit.setId("CMBdebit");
        cmbDebit.setBankCode("CMB");
        cmbDebit.setCardTypeCode("debit");
        cmbDebit.setCardName("招商银行储蓄卡");
        cmbDebit.setDeleted(0);

        when(bankCardService.listByBankAndType("CMB", "debit")).thenReturn(List.of(cmbDebit));

        var txns = service.parseAndEnrichTransactions(
                List.of(new String[]{"记账日期", "货币", "交易金额", "联机余额", "交易摘要", "对手信息"},
                        new String[]{"2026-05-02", "CNY", "-100.00", "900.00", "快捷支付", "测试"}),
                "CMB",
                "debit",
                null,
                null,
                "stmt-1");

        assertEquals(1, txns.size());
        assertEquals("CMBdebit", txns.get(0).getBankCardId());
        assertNotNull(txns.get(0).getBankCardName());
    }
}
