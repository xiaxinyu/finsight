package com.finsight.web.restful.report;

import com.finsight.application.report.TransactionReportFacade;
import com.finsight.core.AppServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionReportFacade transactionReportFacade;

    @Test
    void consumeReport_returnsSuccessWithData() throws Exception {
        when(transactionReportFacade.consumeReportJson(any()))
                .thenReturn("[{\"key\":\"Food\",\"value\":120.5}]");

        mockMvc.perform(post("/transaction-report/consume")
                        .param("transactionDateStartStr", "01/01/2025")
                        .param("transactionDateEndStr", "12/31/2025")
                        .param("txnTypes", "expense"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void monthIncomeReport_returnsSuccessWithData() throws Exception {
        when(transactionReportFacade.monthIncomeReportJson(any()))
                .thenReturn("[{\"key\":\"1\",\"value\":5000}]");

        mockMvc.perform(post("/transaction-report/month-income")
                        .param("transactionDateStartStr", "01/01/2025")
                        .param("transactionDateEndStr", "12/31/2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void monthExpenseReport_returnsSuccessWithData() throws Exception {
        when(transactionReportFacade.monthExpenseReportJson(any()))
                .thenReturn("[{\"key\":\"1\",\"value\":3200}]");

        mockMvc.perform(post("/transaction-report/month-expense")
                        .param("transactionDateStartStr", "01/01/2025")
                        .param("transactionDateEndStr", "12/31/2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void consumeReport_invalidDate_returnsFailure() throws Exception {
        when(transactionReportFacade.consumeReportJson(any()))
                .thenThrow(new AppServiceException("Invalid date format: not-a-date"));

        mockMvc.perform(post("/transaction-report/consume")
                        .param("transactionDateStartStr", "not-a-date")
                        .param("transactionDateEndStr", "12/31/2025")
                        .param("txnTypes", "expense"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50000))
                .andExpect(jsonPath("$.message").value("Invalid date format: not-a-date"));
    }
}
