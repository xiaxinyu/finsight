package com.finsight.application.service;

import com.finsight.core.AppServiceException;
import com.finsight.domain.model.HouseRent;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.HouseRentParam;

/**
 * House rent expense listing for datagrid endpoints.
 */
public interface IHouseRentListingService {

    CollectionResult<HouseRent> listHouseRents(HouseRentParam param) throws AppServiceException;
}
