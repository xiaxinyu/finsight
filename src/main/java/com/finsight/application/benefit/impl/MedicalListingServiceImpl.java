package com.finsight.application.benefit.impl;

import com.finsight.application.benefit.IMedicalListingService;
import com.finsight.application.benefit.IMedicalService;
import com.finsight.application.support.ListingDateSupport;
import com.finsight.common.exception.AppServiceException;
import com.finsight.common.util.StringTool;
import com.finsight.domain.model.Medical;
import com.finsight.domain.model.Page;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.MedicalParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MedicalListingServiceImpl implements IMedicalListingService {

    @Autowired
    private IMedicalService medicalService;

    @Override
    public CollectionResult<Medical> listMedicals(MedicalParam param) throws AppServiceException {
        Medical medical = new Medical();
        String[] ym = ListingDateSupport.monthRangeOrDefaultOneYear(
                param.getTransactionDateStartStr(), param.getTransactionDateEndStr());
        if (ym[0] != null) {
            medical.setTimeFrom(ym[0]);
        }
        if (ym[1] != null) {
            medical.setTimeTo(ym[1]);
        }
        if (!StringTool.isNullOrEmpty(param.getDemoArea())) {
            medical.setDemoArea(param.getDemoArea().trim());
        }
        if (!StringTool.isNullOrEmpty(param.getUnitNo()) && !"0".equals(String.valueOf(param.getUnitNo()).trim())) {
            medical.setUnitNo(String.valueOf(param.getUnitNo()).trim());
        }

        Page page = new Page(param.getPage(), param.getRows());
        CollectionResult<Medical> result = new CollectionResult<>();
        result.setRows(medicalService.getMedicals(medical, page));
        result.setTotal(medicalService.countMedicals(medical));
        return result;
    }
}
