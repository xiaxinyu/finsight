package com.finsight.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;

@Mapper
public interface AccountBalanceSnapshotMapper {

    int upsertSnapshot(@Param("id") String id,
                       @Param("userId") String userId,
                       @Param("cardId") String cardId,
                       @Param("balance") BigDecimal balance,
                       @Param("snapshotDate") Date snapshotDate,
                       @Param("source") String source,
                       @Param("actor") String actor);
}
