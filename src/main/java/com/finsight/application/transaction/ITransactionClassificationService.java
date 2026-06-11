package com.finsight.application.transaction;

import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.Transaction;

import java.util.Optional;

/**
 * REST-oriented helpers for transaction classification (suggest categories, keyword tokens).
 */
public interface ITransactionClassificationService {

    /**
     * Runs classifier for UI; empty when there is no match (maps to {@code no_match} on the controller).
     */
    Optional<String> classifyForApi(Transaction transaction, String bankCode, String cardTypeCode) throws AppServiceException;

    /** JSON array string of keyword tokens (may be {@code []}). */
    String keywordsJson(Transaction transaction) throws AppServiceException;

    /** Suggest-only top-N matches for a single transaction (never persists). */
    Optional<String> suggestTopN(Transaction transaction, String bankCode, String cardTypeCode) throws AppServiceException;
}
