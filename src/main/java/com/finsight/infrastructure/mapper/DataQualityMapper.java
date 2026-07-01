package com.finsight.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface DataQualityMapper {

    Map<String, Object> classificationCoverage(@Param("ownerUserId") String ownerUserId);

    int countOrphanCategoryTransactions(@Param("ownerUserId") String ownerUserId);

    int countRefundExcluded(@Param("ownerUserId") String ownerUserId);

    Map<String, Object> merchantTokenCoverage(@Param("ownerUserId") String ownerUserId);
}
