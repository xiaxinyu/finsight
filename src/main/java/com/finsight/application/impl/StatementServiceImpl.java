package com.finsight.application.impl;

import com.finsight.application.IStatementService;
import com.finsight.domain.model.Statement;
import com.finsight.infrastructure.mapper.StatementMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

@Service
public class StatementServiceImpl extends ServiceImpl<StatementMapper, Statement> implements IStatementService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createStatement(Statement statement, String userName) {
        if (statement.getId() == null) {
            statement.setId(UUID.randomUUID().toString());
        }
        statement.setCreatetime(new Date());
        statement.setUpdatetime(new Date());
        statement.setCreateuser(userName);
        statement.setUpdateuser(userName);
        statement.setDeleted(0);
        this.save(statement);
    }
}
