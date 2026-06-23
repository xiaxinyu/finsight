package com.finsight.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface DataQualityMapper {

    Map<String, Object> classificationCoverage();

    int countOrphanCategoryTransactions();

    int countRefundExcluded();

    Map<String, Object> merchantTokenCoverage();
}
