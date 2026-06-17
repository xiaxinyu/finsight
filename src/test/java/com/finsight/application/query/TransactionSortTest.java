package com.finsight.application.query;

import com.finsight.web.api.dto.TransactionParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionSortTest {

    @Test
    void apply_setsWhitelistedFieldAndOrder() {
        TransactionParam param = new TransactionParam();
        param.setSortField("amount");
        param.setSortOrder("asc");
        TransactionQuery query = new TransactionQuery();

        TransactionSort.apply(param, query);

        assertEquals("amount", query.getSortField());
        assertEquals("asc", query.getSortOrder());
    }

    @Test
    void apply_ignoresInvalidField() {
        TransactionParam param = new TransactionParam();
        param.setSortField("transaction_desc;drop table");
        param.setSortOrder("asc");
        TransactionQuery query = new TransactionQuery();

        TransactionSort.apply(param, query);

        assertNull(query.getSortField());
        assertNull(query.getSortOrder());
    }

    @Test
    void normalizeOrder_defaultsInvalidToDesc() {
        assertEquals("desc", TransactionSort.normalizeOrder(null));
        assertEquals("desc", TransactionSort.normalizeOrder("invalid"));
        assertEquals("asc", TransactionSort.normalizeOrder("ASC"));
    }

    @Test
    void assembler_appliesSortFromParam() throws Exception {
        TransactionParam param = new TransactionParam();
        param.setSortField("card");
        param.setSortOrder("desc");

        TransactionQuery query = TransactionQueryAssembler.from(param);

        assertEquals("card", query.getSortField());
        assertEquals("desc", query.getSortOrder());
    }
}
