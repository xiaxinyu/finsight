package com.finsight.application.rent;

import java.util.List;

import com.finsight.domain.model.HouseRent;
import com.finsight.domain.model.Page;
import com.finsight.common.exception.AppServiceException;

public interface IHouseRentService {
    int countHouseRent(HouseRent houseRent) throws AppServiceException;

    List<HouseRent> getHouseRents(HouseRent houseRent, Page page) throws AppServiceException;
}
