package com.finsight.web.restful.insurance;

import com.finsight.application.benefit.IEndowmentListingService;
import com.finsight.application.benefit.IEndowmentService;
import com.finsight.domain.model.Endowment;
import com.finsight.web.restful.common.ControllerHelper;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.CommonResult;
import com.finsight.web.restful.model.EndowmentParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/endowment")
public class EndowmentResource extends ControllerHelper {
	private static final Logger logger = LoggerFactory.getLogger(EndowmentResource.class);
	
	@Autowired
	private IEndowmentService endowmentService;

	@Autowired
	private IEndowmentListingService endowmentListingService;
	
	@RequestMapping("/getEndowments")
	@ResponseBody 
	public CollectionResult<Endowment> getEndowments(EndowmentParam param){
		return runCollection(logger, "get endowments", () -> endowmentListingService.listEndowments(param));
	}
	
	@RequestMapping("/add")
	@ResponseBody 
	public CommonResult addEndowment(Endowment endowment){
		return runCommon(logger, "add endowment", () -> {
			String userName = getSessionUser().getUserName();
			stampNewRecord(endowment, userName);
			endowmentService.addEndowment(endowment);
			return CommonResult.success(OPERATION_OK);
		});
	}
	
	@RequestMapping("/delete")
	@ResponseBody 
	public CommonResult deleteEndowment(String id) {
		return runCommon(logger, "delete endowment", () -> {
			endowmentService.deleteEndowment(id);
			return CommonResult.success(OPERATION_OK);
		});
	}
	
	@RequestMapping("/update")
	@ResponseBody 
	public CommonResult updateEndowment(Endowment endowment){
		return runCommon(logger, "update endowment", () -> {
			String userName = getSessionUser().getUserName();
			endowment.setUpdateuser(userName);
			endowmentService.updateEndowment(endowment);
			return CommonResult.success(OPERATION_OK);
		});
	}
	
	@RequestMapping("/copy")
	@ResponseBody 
	public CommonResult copyEndowment(Endowment endowment){
		return runCommon(logger, "copy endowment", () -> {
			String userName = getSessionUser().getUserName();
			stampNewRecord(endowment, userName);
			endowmentService.addEndowment(endowment);
			return CommonResult.success(OPERATION_OK);
		});
	}
}
