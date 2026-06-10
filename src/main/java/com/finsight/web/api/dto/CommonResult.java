package com.finsight.web.api.dto;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Created by Summer.Xia on 9/1/2015.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class CommonResult implements Serializable{
	private static final long serialVersionUID = 1L;
	private String returnCode;
	private String returnMessage;
	private Integer code;
	private String message;
	private Object data;

	public CommonResult(String returnCode, String returnMessage) {
		this.returnCode = returnCode;
		this.returnMessage = returnMessage;
		this.code = "success".equals(returnCode) ? 20000 : 50000;
		this.message = returnMessage;
		this.data = returnMessage;
	}

	public static CommonResult success(Object data){
		String text = data == null ? "" : String.valueOf(data);
		CommonResult result = new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), text);
		result.setCode(20000);
		result.setMessage("success");
		result.setData(data);
		return result;
	}

	public static CommonResult fail(String message){
		CommonResult result = new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), message);
		result.setCode(50000);
		result.setMessage(message);
		result.setData(null);
		return result;
	}

	public String getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(String returnCode) {
		this.returnCode = returnCode;
	}

	public String getReturnMessage() {
		return returnMessage;
	}

	public void setReturnMessage(String returnMessage) {
		this.returnMessage = returnMessage;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
}
