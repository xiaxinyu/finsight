package com.finsight.infrastructure.mapper;

import com.finsight.domain.model.KeyValue;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface FinancialMapper {
    void markTransactionsTransfer(@Param("ids") List<String> ids, @Param("groupId") String groupId);

    int countUnclassified();

    int countTransferGroups();

    List<Map<String, Object>> listTransferGroups();

    List<KeyValue> latestBalancesFromBankCards();

    Double sumExpenseSince(@Param("since") Date since);

    Double sumIncomeSince(@Param("since") Date since);

    Double sumFixedBucketYear(@Param("year") int year);

    List<String> findDuplicatePreviewTempIds(@Param("statementId") String statementId);

    List<KeyValue> latestInferredBalancePerCard();

    Double sumExpenseByBucketSince(@Param("since") Date since, @Param("bucketKey") String bucketKey);

    Double sumExpenseByCategorySince(@Param("since") Date since, @Param("categoryCode") String categoryCode);

    Double sumExpenseBetween(@Param("start") Date start, @Param("end") Date end);

    Double sumExpenseByBucketBetween(@Param("start") Date start, @Param("end") Date end,
                                     @Param("bucketKey") String bucketKey);

    Double sumExpenseByCategoryBetween(@Param("start") Date start, @Param("end") Date end,
                                       @Param("categoryCode") String categoryCode);
}
