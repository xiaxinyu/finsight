package com.finsight.infrastructure.mapper;

import com.finsight.domain.model.KeyValue;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface FinancialMapper {
    void markTransactionsTransfer(@Param("ids") List<String> ids, @Param("groupId") String groupId);

    int countUnclassified(@Param("ownerUserId") String ownerUserId);

    int countTransferGroups(@Param("ownerUserId") String ownerUserId);

    List<Map<String, Object>> listTransferGroups(@Param("ownerUserId") String ownerUserId);

    List<KeyValue> latestBalancesFromBankCards(@Param("ownerUserId") String ownerUserId);

    Double sumCurrentLiabilities(@Param("ownerUserId") String ownerUserId);

    Double sumExpenseSince(@Param("since") Date since, @Param("ownerUserId") String ownerUserId);

    Double sumIncomeSince(@Param("since") Date since, @Param("ownerUserId") String ownerUserId);

    Double sumFixedBucketYear(@Param("year") int year, @Param("ownerUserId") String ownerUserId);

    List<String> findDuplicatePreviewTempIds(@Param("statementId") String statementId,
                                             @Param("ownerUserId") String ownerUserId);

    List<KeyValue> latestInferredBalancePerCard(@Param("ownerUserId") String ownerUserId);

    Double sumExpenseByBucketSince(@Param("since") Date since, @Param("bucketKey") String bucketKey,
                                   @Param("ownerUserId") String ownerUserId);

    Double sumExpenseByCategorySince(@Param("since") Date since, @Param("categoryCode") String categoryCode,
                                     @Param("ownerUserId") String ownerUserId);

    Double sumExpenseBetween(@Param("start") Date start, @Param("end") Date end,
                             @Param("ownerUserId") String ownerUserId);

    Double sumExpenseByBucketBetween(@Param("start") Date start, @Param("end") Date end,
                                     @Param("bucketKey") String bucketKey,
                                     @Param("ownerUserId") String ownerUserId);

    Double sumExpenseByCategoryBetween(@Param("start") Date start, @Param("end") Date end,
                                       @Param("categoryCode") String categoryCode,
                                       @Param("ownerUserId") String ownerUserId);
}
