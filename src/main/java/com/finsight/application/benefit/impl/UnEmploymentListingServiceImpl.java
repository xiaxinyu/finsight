package com.finsight.application.benefit.impl;

import com.finsight.application.benefit.IUnEmploymentListingService;
import com.finsight.application.benefit.IUnEmploymentService;
import com.finsight.application.support.ListingDateSupport;
import com.finsight.core.AppServiceException;
import com.finsight.core.StringTool;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.UnEmployment;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.UnEmploymentParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UnEmploymentListingServiceImpl implements IUnEmploymentListingService {

    @Autowired
    private IUnEmploymentService unEmploymentService;

    @Override
    public CollectionResult<UnEmployment> listUnEmployments(UnEmploymentParam param) throws AppServiceException {
        UnEmployment unEmployment = new UnEmployment();
        String[] ym = ListingDateSupport.monthRangeOrDefaultOneYear(
                param.getTransactionDateStartStr(), param.getTransactionDateEndStr());
        if (ym[0] != null) {
            unEmployment.setTimeFrom(ym[0]);
        }
        if (ym[1] != null) {
            unEmployment.setTimeTo(ym[1]);
        }
        if (!StringTool.isNullOrEmpty(param.getDemoArea())) {
            unEmployment.setDemoArea(param.getDemoArea().trim());
        }
        if (!StringTool.isNullOrEmpty(param.getUnitNo()) && !"0".equals(String.valueOf(param.getUnitNo()).trim())) {
            unEmployment.setUnitNo(String.valueOf(param.getUnitNo()).trim());
        }

        Page page = new Page(param.getPage(), param.getRows());
        CollectionResult<UnEmployment> result = new CollectionResult<>();
        result.setRows(unEmploymentService.getUnEmployments(unEmployment, page));
        result.setTotal(unEmploymentService.countUnEmployments(unEmployment));
        return result;
    }
}
