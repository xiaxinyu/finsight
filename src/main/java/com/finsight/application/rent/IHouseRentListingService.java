package com.finsight.application.rent;

import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.HouseRent;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.HouseRentParam;

/**
 * House rent expense listing for datagrid endpoints.
 */
public interface IHouseRentListingService {

    CollectionResult<HouseRent> listHouseRents(HouseRentParam param) throws AppServiceException;
}
