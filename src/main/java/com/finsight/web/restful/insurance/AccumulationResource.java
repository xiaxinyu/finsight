package com.finsight.web.restful.insurance;
import com.finsight.web.restful.common.ControllerHelper;

import com.alibaba.fastjson.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.finsight.core.StringTool;
import com.finsight.domain.model.Accumulation;
import com.finsight.domain.model.Page;
import com.finsight.core.AppServiceException;
import com.finsight.application.service.IAccumulationService;
import com.finsight.web.restful.model.AccumulationParam;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.CommonResult;
import com.finsight.web.restful.model.ResultCode;

@Controller
@RequestMapping("/accumulation")
public class AccumulationResource extends ControllerHelper {
	private static final Logger logger = LoggerFactory.getLogger(AccumulationResource.class);
	
	@Autowired
	private IAccumulationService accumulationService;
	
	@RequestMapping("/getAccumulations")
	@ResponseBody 
	public CollectionResult<Accumulation> getAccumulations(AccumulationParam param){
		try {
			//Fetch params
			Accumulation accumulation = new Accumulation();
			Page page = new Page(param.getPage(),param.getRows());
			CollectionResult<Accumulation> result = new CollectionResult<Accumulation>();
			result.setRows(accumulationService.getAccumulations(accumulation,page));
			result.setTotal(accumulationService.countAccumulations(accumulation));
			return result;
		} catch (AppServiceException e) {
			logger.error("get Accumulations failed. params[message = " + e.getMessage() + "]", e);
		} 
		return null;
	}
	
	@RequestMapping("/add")
	@ResponseBody 
	public String addAccumulation(Accumulation accumulation){
		try {
			String userName = this.getSessionUser().getUserName();
			accumulation.setId(StringTool.generateID());
			accumulation.setCreateuser(userName);
			accumulation.setUpdateuser(userName);
			accumulationService.addAccumulation(accumulation);
			return JSONObject.toJSONString(new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), "操作成功."));
		} catch (AppServiceException e) {
			logger.error("add Accumulation failed. params[UnitNo = " + accumulation.getUnitNo() + ",Time = " + accumulation.getTime() + "]", e);
			return JSONObject.toJSONString(new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage()));
		}
	}
	
	@RequestMapping("/delete")
	@ResponseBody 
	public CommonResult deleteAccumulation(String id) {
		try {
			accumulationService.deleteAccumulation(id);
			return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), "操作成功.");
		} catch (AppServiceException e) {
			logger.error("delete Accumulation failed. params[id = " + id + "]", e);
			return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage());
		}
	}
	
	@RequestMapping("/update")
	@ResponseBody 
	public CommonResult updateAccumulation(Accumulation accumulation){
		try {
			String userName = this.getSessionUser().getUserName();
			accumulation.setUpdateuser(userName);
			accumulationService.updateAccumulation(accumulation);
			return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), "操作成功.");
		} catch (AppServiceException e) {
			logger.error("update Accumulation failed. params[id = " + accumulation.getId() + "]", e);
			return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage());
		}
	}
	
	@RequestMapping("/copy")
	@ResponseBody 
	public CommonResult copyAccumulation(Accumulation accumulation){
		try {
			String userName = this.getSessionUser().getUserName();
			accumulation.setId(StringTool.generateID());
			accumulation.setCreateuser(userName);
			accumulation.setUpdateuser(userName);
			accumulation.setUpdateuser(userName);
			accumulationService.addAccumulation(accumulation);
			return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), "操作成功.");
		} catch (AppServiceException e) {
			logger.error("update Accumulation failed. params[id = " + accumulation.getId() + "]", e);
			return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), e.getMessage());
		}
	}
}
