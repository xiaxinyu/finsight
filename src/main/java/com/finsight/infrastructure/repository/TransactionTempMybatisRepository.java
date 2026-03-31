package com.finsight.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finsight.domain.model.TransactionTemp;
import com.finsight.domain.port.TransactionTempRepository;
import com.finsight.infrastructure.mapper.TransactionTempMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class TransactionTempMybatisRepository implements TransactionTempRepository {

    @Autowired
    private TransactionTempMapper transactionTempMapper;

    @Override
    public List<TransactionTemp> findByStatementId(String statementId) {
        if (StringUtils.isBlank(statementId)) {
            return Collections.emptyList();
        }
        QueryWrapper<TransactionTemp> qw = new QueryWrapper<>();
        qw.eq("recordID", statementId);
        return transactionTempMapper.selectList(qw);
    }

    @Override
    public void softDeleteByStatementId(String statementId) {
        if (StringUtils.isBlank(statementId)) {
            return;
        }
        QueryWrapper<TransactionTemp> qw = new QueryWrapper<>();
        qw.eq("recordID", statementId);
        TransactionTemp update = new TransactionTemp();
        update.setDeleted(1);
        transactionTempMapper.update(update, qw);
    }

    @Override
    public void saveBatch(List<TransactionTemp> temps) {
        if (temps == null || temps.isEmpty()) {
            return;
        }
        for (TransactionTemp t : temps) {
            if (t == null) {
                continue;
            }
            transactionTempMapper.insert(t);
        }
    }
}

