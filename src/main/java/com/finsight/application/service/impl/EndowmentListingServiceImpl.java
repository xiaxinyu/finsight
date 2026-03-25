package com.finsight.application.service.impl;

import com.finsight.application.service.IEndowmentListingService;
import com.finsight.application.service.IEndowmentService;
import com.finsight.application.service.support.ListingDateSupport;
import com.finsight.core.AppServiceException;
import com.finsight.core.StringTool;
import com.finsight.domain.model.Endowment;
import com.finsight.domain.model.Page;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.EndowmentParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EndowmentListingServiceImpl implements IEndowmentListingService {

    @Autowired
    private IEndowmentService endowmentService;

    @Override
    public CollectionResult<Endowment> listEndowments(EndowmentParam param) throws AppServiceException {
        Endowment endowment = new Endowment();
        String[] ym = ListingDateSupport.monthRangeOrNull(
                param.getTransactionDateStartStr(), param.getTransactionDateEndStr());
        if (ym[0] != null) {
            endowment.setTimeFrom(ym[0]);
        }
        if (ym[1] != null) {
            endowment.setTimeTo(ym[1]);
        }
        if (!StringTool.isNullOrEmpty(param.getDemoArea())) {
            endowment.setDemoArea(param.getDemoArea().trim());
        }
        if (!StringTool.isNullOrEmpty(param.getUnitNo()) && !"0".equals(String.valueOf(param.getUnitNo()).trim())) {
            endowment.setUnitNo(String.valueOf(param.getUnitNo()).trim());
        }

        Page page = new Page(param.getPage(), param.getRows());
        CollectionResult<Endowment> result = new CollectionResult<>();
        result.setRows(endowmentService.getEndowments(endowment, page));
        result.setTotal(endowmentService.countEndowments(endowment));
        return result;
    }
}
