package com.finsight.infrastructure.mapper;

import com.finsight.domain.model.KeyValue;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface FinancialMapper {
    void markTransactionsTransfer(@Param("ids") List<String> ids, @Param("groupId") String groupId);

    int countUnclassified();

    int countDuplicateFingerprints();

    List<KeyValue> latestAccountBalances();

    Double sumExpenseSince(@Param("since") Date since);

    Double sumIncomeSince(@Param("since") Date since);

    Double sumFixedBucketYear(@Param("year") int year);

    List<String> findDuplicatePreviewFingerprints(@Param("bankCardId") String bankCardId);

    List<String> findDuplicatePreviewTempIds(@Param("statementId") String statementId);
}
