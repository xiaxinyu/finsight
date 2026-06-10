package com.finsight.web.restful.insurance;

import com.finsight.application.benefit.IMedicalListingService;
import com.finsight.application.benefit.IMedicalService;
import com.finsight.domain.model.Medical;
import com.finsight.web.restful.common.ControllerHelper;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.CommonResult;
import com.finsight.web.restful.model.MedicalParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/medical")
public class MedicalResource extends ControllerHelper {
	private static final Logger logger = LoggerFactory.getLogger(MedicalResource.class);
	
	@Autowired
	private IMedicalService medicalService;

	@Autowired
	private IMedicalListingService medicalListingService;
	
	@RequestMapping("/getMedicals")
	@ResponseBody 
	public CollectionResult<Medical> getMedicals(MedicalParam param){
		return runCollection(logger, "get medicals", () -> medicalListingService.listMedicals(param));
	}
	
	@RequestMapping("/add")
	@ResponseBody 
	public CommonResult addMedical(Medical medical){
		return runCommon(logger, "add medical", () -> {
			String userName = getSessionUser().getUserName();
			stampNewRecord(medical, userName);
			medicalService.addMedical(medical);
			return CommonResult.success(OPERATION_OK);
		});
	}
	
	@RequestMapping("/delete")
	@ResponseBody 
	public CommonResult deleteMedical(String id) {
		return runCommon(logger, "delete medical", () -> {
			medicalService.deleteMedical(id);
			return CommonResult.success(OPERATION_OK);
		});
	}
	
	@RequestMapping("/update")
	@ResponseBody 
	public CommonResult updateMedical(Medical medical){
		return runCommon(logger, "update medical", () -> {
			String userName = getSessionUser().getUserName();
			medical.setUpdateuser(userName);
			medicalService.updateMedical(medical);
			return CommonResult.success(OPERATION_OK);
		});
	}
	
	@RequestMapping("/copy")
	@ResponseBody 
	public CommonResult copyMedical(Medical medical){
		return runCommon(logger, "copy medical", () -> {
			String userName = getSessionUser().getUserName();
			stampNewRecord(medical, userName);
			medicalService.addMedical(medical);
			return CommonResult.success(OPERATION_OK);
		});
	}
}
