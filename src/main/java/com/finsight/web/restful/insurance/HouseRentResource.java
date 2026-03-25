package com.finsight.web.restful.insurance;

import com.finsight.application.service.IHouseRentListingService;
import com.finsight.domain.model.HouseRent;
import com.finsight.web.restful.common.ControllerHelper;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.HouseRentParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/house-rent")
public class HouseRentResource extends ControllerHelper {

    private static final Logger logger = LoggerFactory.getLogger(HouseRentResource.class);

    @Autowired
    private IHouseRentListingService houseRentListingService;

    @RequestMapping("/getHouseRents")
    @ResponseBody
    public CollectionResult<HouseRent> getHouseRents(HouseRentParam param) {
        return runCollection(logger, "get house rents", () -> houseRentListingService.listHouseRents(param));
    }
}
