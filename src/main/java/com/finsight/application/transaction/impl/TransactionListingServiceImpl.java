package com.finsight.application.transaction.impl;

import com.finsight.application.transaction.ITransactionListingService;
import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.application.query.TransactionQuery;
import com.finsight.application.query.TransactionQueryAssembler;
import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.TransactionParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class TransactionListingServiceImpl implements ITransactionListingService {

    private static final Logger log = LoggerFactory.getLogger(TransactionListingServiceImpl.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionQuerySupport transactionQuerySupport;

    @Override
    public CollectionResult<Transaction> listTransactions(TransactionParam param) throws AppServiceException {
        TransactionQuery query = TransactionQueryAssembler.from(param);
        transactionQuerySupport.enrich(query);
        Page page = new Page(param.getPage(), param.getRows());

        CollectionResult<Transaction> result = new CollectionResult<>();
        StopWatch stopWatch = new StopWatch("消费数据查询统计");
        stopWatch.start("查询列表数据");
        result.setRows(transactionRepository.getTransactions(query, page));
        enrichTransactionDateTime(result.getRows());
        stopWatch.stop();

        stopWatch.start("查询统计数据");
        result.setTotal(transactionRepository.countTransaction(query));
        stopWatch.stop();

        log.info("耗时打印：{}", stopWatch.prettyPrint());
        return result;
    }

    private static void enrichTransactionDateTime(List<Transaction> rows) {
        if (rows == null) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (Transaction t : rows) {
            try {
                String ds = t.getTransactionDate() == null ? "" : sdf.format(t.getTransactionDate());
                String ts = StringUtils.trimToEmpty(t.getTransactionTime());
                t.setTransactionDateTime(StringUtils.isNotBlank(ts) ? (ds + " " + ts) : ds);
            } catch (Exception ignore) {
                // keep row as-is
            }
        }
    }

}
