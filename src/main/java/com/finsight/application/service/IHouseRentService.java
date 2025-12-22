package com.finsight.application.service;

import java.util.List;

import com.finsight.domain.model.HouseRent;
import com.finsight.domain.model.Page;
import com.finsight.core.AppServiceException;

public interface IHouseRentService {
    int countHouseRent(HouseRent houseRent) throws AppServiceException;

    List<HouseRent> getHouseRents(HouseRent houseRent, Page page) throws AppServiceException;
}
