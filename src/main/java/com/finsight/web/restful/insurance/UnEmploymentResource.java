package com.finsight.web.restful.insurance;

import com.finsight.application.service.IUnEmploymentListingService;
import com.finsight.application.service.IUnEmploymentService;
import com.finsight.domain.model.UnEmployment;
import com.finsight.web.restful.common.ControllerHelper;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.CommonResult;
import com.finsight.web.restful.model.UnEmploymentParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/unemployment")
public class UnEmploymentResource extends ControllerHelper {
    private static final Logger logger = LoggerFactory.getLogger(UnEmploymentResource.class);

    @Autowired
    private IUnEmploymentService unEmploymentService;

    @Autowired
    private IUnEmploymentListingService unEmploymentListingService;

    @RequestMapping("/getUnEmployments")
    @ResponseBody
    public CollectionResult<UnEmployment> getUnEmployments(UnEmploymentParam param) {
        return runCollection(logger, "get unemployments", () -> unEmploymentListingService.listUnEmployments(param));
    }

    @RequestMapping("/add")
    @ResponseBody
    public CommonResult addUnEmployment(UnEmployment unEmployment) {
        return runCommon(logger, "add unemployment", () -> {
            String userName = getSessionUser().getUserName();
            stampNewRecord(unEmployment, userName);
            unEmploymentService.addUnEmployment(unEmployment);
            return CommonResult.success(OPERATION_OK);
        });
    }

    @RequestMapping("/delete")
    @ResponseBody
    public CommonResult deleteUnEmployment(String id) {
        return runCommon(logger, "delete unemployment", () -> {
            unEmploymentService.deleteUnEmployment(id);
            return CommonResult.success(OPERATION_OK);
        });
    }

    @RequestMapping("/update")
    @ResponseBody
    public CommonResult updateUnEmployment(UnEmployment unEmployment) {
        return runCommon(logger, "update unemployment", () -> {
            String userName = getSessionUser().getUserName();
            unEmployment.setUpdateuser(userName);
            unEmploymentService.updateUnEmployment(unEmployment);
            return CommonResult.success(OPERATION_OK);
        });
    }

    @RequestMapping("/copy")
    @ResponseBody
    public CommonResult copyUnEmployment(UnEmployment unEmployment) {
        return runCommon(logger, "copy unemployment", () -> {
            String userName = getSessionUser().getUserName();
            stampNewRecord(unEmployment, userName);
            unEmploymentService.addUnEmployment(unEmployment);
            return CommonResult.success(OPERATION_OK);
        });
    }
}
