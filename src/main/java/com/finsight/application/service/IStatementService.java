package com.finsight.application.service;

import com.finsight.domain.model.Statement;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IStatementService extends IService<Statement> {
    void createStatement(Statement statement, String userName);
}
