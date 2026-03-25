package com.finsight.application.service.impl;

import com.finsight.application.service.IHouseRentListingService;
import com.finsight.application.service.IHouseRentService;
import com.finsight.application.service.support.ListingDateSupport;
import com.finsight.core.AppServiceException;
import com.finsight.core.StringTool;
import com.finsight.domain.model.HouseRent;
import com.finsight.domain.model.Page;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.HouseRentParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Maps request parameters to {@link HouseRent} query model and returns paged results.
 */
@Service
public class HouseRentListingServiceImpl implements IHouseRentListingService {

    @Autowired
    private IHouseRentService houseRentService;

    @Override
    public CollectionResult<HouseRent> listHouseRents(HouseRentParam param) throws AppServiceException {
        HouseRent houseRent = new HouseRent();
        if (!StringTool.isNullOrEmpty(param.getTransactionDateStartStr())) {
            houseRent.setTransactionDateStart(
                    ListingDateSupport.parseMmDdYyyy(param.getTransactionDateStartStr()));
        }
        if (!StringTool.isNullOrEmpty(param.getTransactionDateEndStr())) {
            houseRent.setTransactionDateEnd(
                    ListingDateSupport.parseMmDdYyyy(param.getTransactionDateEndStr()));
        }
        if (!StringTool.isNullOrEmpty(param.getDemoArea())) {
            houseRent.setDemoArea(param.getDemoArea());
        }
        if (!StringTool.isNullOrEmpty(param.getCardTypeName())) {
            houseRent.setCardTypeName(param.getCardTypeName());
        }
        if (!StringTool.isNullOrEmpty(param.getYear())) {
            houseRent.setYear(param.getYear());
        }

        Page page = new Page(param.getPage(), param.getRows());
        CollectionResult<HouseRent> result = new CollectionResult<>();
        result.setRows(houseRentService.getHouseRents(houseRent, page));
        result.setTotal(houseRentService.countHouseRent(houseRent));
        return result;
    }
}
