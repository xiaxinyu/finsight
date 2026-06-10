package com.finsight.web.api.benefit;

import com.finsight.application.rent.IHouseRentListingService;
import com.finsight.domain.model.HouseRent;
import com.finsight.web.api.support.ControllerHelper;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.HouseRentParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/house-rent")
public class HouseRentController extends ControllerHelper {

    private static final Logger logger = LoggerFactory.getLogger(HouseRentController.class);

    @Autowired
    private IHouseRentListingService houseRentListingService;

    @RequestMapping("/getHouseRents")
    @ResponseBody
    public CollectionResult<HouseRent> getHouseRents(HouseRentParam param) {
        return runCollection(logger, "get house rents", () -> houseRentListingService.listHouseRents(param));
    }
}
