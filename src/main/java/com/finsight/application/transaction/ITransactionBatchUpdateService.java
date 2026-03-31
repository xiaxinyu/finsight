package com.finsight.application.service;

import java.util.Optional;

/**
 * Batch category updates for transactions from JSON payloads (e.g. datagrid save).
 */
public interface ITransactionBatchUpdateService {

    /**
     * Parses payload as a JSON array of {@link com.finsight.domain.model.Transaction}, applies updates, returns
     * a JSON string {@code {"success":n,"failed":m}} for {@link com.finsight.web.restful.model.CommonResult#success(Object)}.
     *
     * @return empty if payload was blank or produced no rows (caller maps to {@code empty_payload})
     */
    Optional<String> batchUpdateTransactions(String payload, String userName);
}
