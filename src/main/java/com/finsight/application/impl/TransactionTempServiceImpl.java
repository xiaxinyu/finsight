package com.finsight.application.impl;

import com.finsight.application.ITransactionTempService;
import com.finsight.domain.model.TransactionTemp;
import com.finsight.infrastructure.mapper.TransactionTempMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TransactionTempServiceImpl extends ServiceImpl<TransactionTempMapper, TransactionTemp> implements ITransactionTempService {

    @Override
    public List<TransactionTemp> getByStatementId(String statementId) {
        QueryWrapper<TransactionTemp> query = new QueryWrapper<>();
        query.eq("recordID", statementId);
        return list(query);
    }

    @Override
    public void deleteByStatementId(String statementId) {
        QueryWrapper<TransactionTemp> query = new QueryWrapper<>();
        query.eq("recordID", statementId);
        // Soft delete: set deleted = 1
        TransactionTemp updateEntity = new TransactionTemp();
        updateEntity.setDeleted(1);
        update(updateEntity, query);
    }
}
