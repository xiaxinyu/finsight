package com.finsight.application.benefit.impl;

import com.finsight.application.authentication.LedgerUserScope;
import com.finsight.application.benefit.IAccumulationListingService;
import com.finsight.application.benefit.IAccumulationService;
import com.finsight.application.support.ListingDateSupport;
import com.finsight.common.exception.AppServiceException;
import com.finsight.common.util.StringTool;
import com.finsight.domain.model.Accumulation;
import com.finsight.domain.model.Page;
import com.finsight.web.api.dto.AccumulationParam;
import com.finsight.web.api.dto.CollectionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccumulationListingServiceImpl implements IAccumulationListingService {

    @Autowired
    private IAccumulationService accumulationService;

    @Autowired
    private LedgerUserScope ledgerUserScope;

    @Override
    public CollectionResult<Accumulation> listAccumulations(AccumulationParam param) throws AppServiceException {
        Accumulation accumulation = new Accumulation();
        accumulation.setCreatedBy(ledgerUserScope.resolve());
        String[] ym = ListingDateSupport.monthRangeOrDefaultOneYear(
                param.getTransactionDateStartStr(), param.getTransactionDateEndStr());
        if (ym[0] != null) {
            accumulation.setTimeFrom(ym[0]);
        }
        if (ym[1] != null) {
            accumulation.setTimeTo(ym[1]);
        }
        if (!StringTool.isNullOrEmpty(param.getDemoArea())) {
            accumulation.setDemoArea(param.getDemoArea().trim());
        }
        if (!StringTool.isNullOrEmpty(param.getUnitNo()) && !"0".equals(String.valueOf(param.getUnitNo()).trim())) {
            accumulation.setUnitNo(String.valueOf(param.getUnitNo()).trim());
        }

        Page page = new Page(param.getPage(), param.getRows());
        CollectionResult<Accumulation> result = new CollectionResult<>();
        result.setRows(accumulationService.getAccumulations(accumulation, page));
        result.setTotal(accumulationService.countAccumulations(accumulation));
        return result;
    }
}
