package com.finsight.application.classification;

import com.finsight.common.util.StringTool;
import com.finsight.domain.model.ClassificationMigrationBatch;
import com.finsight.domain.model.ClassificationMigrationDetail;
import com.finsight.infrastructure.mapper.ClassificationMigrationBatchMapper;
import com.finsight.infrastructure.mapper.ClassificationMigrationDetailMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClassificationMigrationBatchService {

    public static final String STATUS_PREVIEW = "PREVIEW";
    public static final String STATUS_APPLIED = "APPLIED";

    private final ClassificationMigrationBatchMapper batchMapper;
    private final ClassificationMigrationDetailMapper detailMapper;

    public ClassificationMigrationBatchService(ClassificationMigrationBatchMapper batchMapper,
                                               ClassificationMigrationDetailMapper detailMapper) {
        this.batchMapper = batchMapper;
        this.detailMapper = detailMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public ClassificationMigrationBatch createBatch(String batchType, String reason, String userName,
                                                    List<ClassificationMigrationDetail> details) {
        ClassificationMigrationBatch batch = new ClassificationMigrationBatch();
        batch.setId(StringTool.generateID());
        batch.setBatchType(StringUtils.defaultIfBlank(batchType, "RECLASSIFY"));
        batch.setStatus(STATUS_PREVIEW);
        batch.setReason(reason);
        batch.setRowCount(details == null ? 0 : details.size());
        batch.setCreatedBy(userName);
        batchMapper.insert(batch);
        if (details != null) {
            for (ClassificationMigrationDetail detail : details) {
                detail.setId(StringTool.generateID());
                detail.setBatchId(batch.getId());
                detailMapper.insert(detail);
            }
        }
        return batch;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markApplied(String batchId) {
        ClassificationMigrationBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            return;
        }
        batch.setStatus(STATUS_APPLIED);
        batch.setAppliedAt(new Date());
        batchMapper.updateById(batch);
    }

    public Map<String, Object> getBatch(String batchId) {
        ClassificationMigrationBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            return Map.of();
        }
        List<ClassificationMigrationDetail> details = detailMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClassificationMigrationDetail>()
                        .eq(ClassificationMigrationDetail::getBatchId, batchId));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("batch", batch);
        out.put("details", details == null ? List.of() : details);
        return out;
    }

    public List<Map<String, Object>> listRecent(int limit) {
        int cap = limit <= 0 ? 20 : Math.min(limit, 100);
        List<ClassificationMigrationBatch> batches = batchMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClassificationMigrationBatch>()
                        .orderByDesc(ClassificationMigrationBatch::getCreatedAt)
                        .last("limit " + cap));
        List<Map<String, Object>> out = new ArrayList<>();
        for (ClassificationMigrationBatch batch : batches) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", batch.getId());
            row.put("batchType", batch.getBatchType());
            row.put("status", batch.getStatus());
            row.put("rowCount", batch.getRowCount());
            row.put("reason", batch.getReason());
            row.put("createdBy", batch.getCreatedBy());
            row.put("createdAt", batch.getCreatedAt());
            row.put("appliedAt", batch.getAppliedAt());
            out.add(row);
        }
        return out;
    }
}
