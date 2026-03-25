package com.finsight.application.service.impl;

import com.finsight.application.service.IAccumulationListingService;
import com.finsight.application.service.IAccumulationService;
import com.finsight.application.service.support.ListingDateSupport;
import com.finsight.core.AppServiceException;
import com.finsight.core.StringTool;
import com.finsight.domain.model.Accumulation;
import com.finsight.domain.model.Page;
import com.finsight.web.restful.model.AccumulationParam;
import com.finsight.web.restful.model.CollectionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccumulationListingServiceImpl implements IAccumulationListingService {

    @Autowired
    private IAccumulationService accumulationService;

    @Override
    public CollectionResult<Accumulation> listAccumulations(AccumulationParam param) throws AppServiceException {
        Accumulation accumulation = new Accumulation();
        String[] ym = ListingDateSupport.monthRangeOrNull(
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
