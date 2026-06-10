package com.finsight.application.rent.impl;

import com.finsight.application.rent.IHouseRentListingService;
import com.finsight.application.rent.IHouseRentService;
import com.finsight.application.support.ListingDateSupport;
import com.finsight.common.exception.AppServiceException;
import com.finsight.common.util.StringTool;
import com.finsight.domain.model.HouseRent;
import com.finsight.domain.model.Page;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.HouseRentParam;
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
        java.util.Date[] range = ListingDateSupport.parseMmDdYyyyOrDefaultOneYear(
                param.getTransactionDateStartStr(), param.getTransactionDateEndStr());
        houseRent.setTransactionDateStart(range[0]);
        houseRent.setTransactionDateEnd(range[1]);
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
