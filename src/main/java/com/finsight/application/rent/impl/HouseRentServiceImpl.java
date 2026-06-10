package com.finsight.application.rent.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.finsight.common.util.DataStructureTool;
import com.finsight.common.util.DateTool;
import com.finsight.infrastructure.mapper.HouseRentMapper;
import com.finsight.domain.model.HouseRent;
import com.finsight.domain.model.Page;
import com.finsight.common.exception.AppServiceException;
import com.finsight.application.rent.IHouseRentService;

/**
 * Created by Summer.Xia on 12/12/2018.
 */
@Service("houseRentService")
public class HouseRentServiceImpl implements IHouseRentService {
    @Autowired
    private HouseRentMapper houseRentMapper;

    public List<HouseRent> getHouseRents(HouseRent houseRent, Page page) throws AppServiceException {
        List<HouseRent> result = null;
        try {
            result = houseRentMapper.getHouseRents(houseRent, page);
            if (DataStructureTool.isNotEmpty(result)) {
                for (HouseRent item : result) {
                    Date transactionDate = item.getTransactionDate();
                    item.setYear("--");
                    if (null != transactionDate) {
                        item.setYear(DateTool.changeDateToString(transactionDate, DateTool.DF_YYYY));
                    }
                }
            }
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }

    public int countHouseRent(HouseRent houseRent) throws AppServiceException {
        int result = 0;
        try {
            result = houseRentMapper.countHouseRent(houseRent);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }
}
