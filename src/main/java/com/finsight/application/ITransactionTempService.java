package com.finsight.application;

import com.finsight.domain.model.TransactionTemp;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface ITransactionTempService extends IService<TransactionTemp> {
    List<TransactionTemp> getByStatementId(String statementId);
    void deleteByStatementId(String statementId);
}
