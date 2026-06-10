package com.finsight.web.api.benefit;

import com.finsight.application.benefit.IAccumulationListingService;
import com.finsight.application.benefit.IAccumulationService;
import com.finsight.domain.model.Accumulation;
import com.finsight.web.api.support.ControllerHelper;
import com.finsight.web.api.dto.AccumulationParam;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.CommonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/accumulation")
public class AccumulationController extends ControllerHelper {
	private static final Logger logger = LoggerFactory.getLogger(AccumulationController.class);
	
	@Autowired
	private IAccumulationService accumulationService;

	@Autowired
	private IAccumulationListingService accumulationListingService;
	
	@RequestMapping("/getAccumulations")
	@ResponseBody 
	public CollectionResult<Accumulation> getAccumulations(AccumulationParam param){
		return runCollection(logger, "get accumulations", () -> accumulationListingService.listAccumulations(param));
	}
	
	@RequestMapping("/add")
	@ResponseBody 
	public CommonResult addAccumulation(Accumulation accumulation){
		return runCommon(logger, "add accumulation", () -> {
			String userName = getSessionUser().getUserName();
			stampNewRecord(accumulation, userName);
			accumulationService.addAccumulation(accumulation);
			return CommonResult.success(OPERATION_OK);
		});
	}
	
	@RequestMapping("/delete")
	@ResponseBody 
	public CommonResult deleteAccumulation(String id) {
		return runCommon(logger, "delete accumulation", () -> {
			accumulationService.deleteAccumulation(id);
			return CommonResult.success(OPERATION_OK);
		});
	}
	
	@RequestMapping("/update")
	@ResponseBody 
	public CommonResult updateAccumulation(Accumulation accumulation){
		return runCommon(logger, "update accumulation", () -> {
			String userName = getSessionUser().getUserName();
			accumulation.setUpdateuser(userName);
			accumulationService.updateAccumulation(accumulation);
			return CommonResult.success(OPERATION_OK);
		});
	}
	
	@RequestMapping("/copy")
	@ResponseBody 
	public CommonResult copyAccumulation(Accumulation accumulation){
		return runCommon(logger, "copy accumulation", () -> {
			String userName = getSessionUser().getUserName();
			stampNewRecord(accumulation, userName);
			accumulationService.addAccumulation(accumulation);
			return CommonResult.success(OPERATION_OK);
		});
	}
}
