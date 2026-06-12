package com.finsight.application.report;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.application.transaction.ITransactionService;
import com.finsight.domain.model.CategoryAggregate;
import com.finsight.domain.model.KeyValue;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.web.api.dto.TransactionParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionReportSnapshotTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ITransactionService transactionService;

    @Mock
    private TransactionQuerySupport transactionQuerySupport;

    @InjectMocks
    private TransactionReportFacade facade;

    @BeforeEach
    void stubQuerySupport() {
        doAnswer(inv -> {
            inv.getArgument(0);
            return null;
        }).when(transactionQuerySupport).enrich(any());
    }

    @Test
    void consumeReportJson_matchesFixtureSnapshot() throws Exception {
        when(transactionRepository.consumeReport(any())).thenReturn(List.of(
                category("FOOD", "餐饮", 1200.50),
                category("TRANSPORT", "交通", 350.00)
        ));
        TransactionParam param = new TransactionParam();
        param.setTransactionDateStartStr("01/01/2025");
        param.setTransactionDateEndStr("12/31/2025");

        JSONArray json = JSON.parseArray(facade.consumeReportJson(param));
        assertEquals(2, json.size());
        assertEquals(1200.50, json.getJSONObject(0).getDoubleValue("value"), 0.001);
        assertEquals("餐饮", json.getJSONObject(0).getString("name"));
    }

    @Test
    void monthIncomeReportJson_matchesFixtureSnapshot() throws Exception {
        when(transactionRepository.monthIncomeReport(any())).thenReturn(monthSeries(1000, 1100, 1200));
        TransactionParam param = new TransactionParam();
        param.setTransactionDateStartStr("01/01/2025");
        param.setTransactionDateEndStr("12/31/2025");

        JSONArray json = JSON.parseArray(facade.monthIncomeReportJson(param));
        assertEquals(12, json.size());
        assertEquals(1000.0, Double.parseDouble(json.getJSONObject(0).getString("value")), 0.001);
        assertEquals(1200.0, Double.parseDouble(json.getJSONObject(2).getString("value")), 0.001);
    }

    @Test
    void monthExpenseReportJson_matchesFixtureSnapshot() throws Exception {
        when(transactionRepository.monthExpenseReport(any())).thenReturn(monthSeries(800, 900, 750));
        TransactionParam param = new TransactionParam();
        param.setTransactionDateStartStr("01/01/2025");
        param.setTransactionDateEndStr("12/31/2025");

        JSONArray json = JSON.parseArray(facade.monthExpenseReportJson(param));
        assertEquals(12, json.size());
        assertEquals(800.0, Double.parseDouble(json.getJSONObject(0).getString("value")), 0.001);
        assertEquals(750.0, Double.parseDouble(json.getJSONObject(2).getString("value")), 0.001);
    }

    private static CategoryAggregate category(String code, String name, double value) {
        CategoryAggregate c = new CategoryAggregate();
        c.setCode(code);
        c.setName(name);
        c.setValue(value);
        return c;
    }

    private static List<KeyValue> monthSeries(double... values) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        KeyValue[] out = new KeyValue[12];
        for (int i = 0; i < 12; i++) {
            KeyValue kv = new KeyValue();
            kv.setKey(months[i]);
            kv.setValue(String.valueOf(i < values.length ? values[i] : 0));
            out[i] = kv;
        }
        return List.of(out);
    }
}
