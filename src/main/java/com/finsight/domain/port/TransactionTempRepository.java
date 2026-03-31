package com.finsight.domain.port;

import com.finsight.domain.model.TransactionTemp;

import java.util.List;

public interface TransactionTempRepository {
    List<TransactionTemp> findByStatementId(String statementId);

    void softDeleteByStatementId(String statementId);

    void saveBatch(List<TransactionTemp> temps);
}

