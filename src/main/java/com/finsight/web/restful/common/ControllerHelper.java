package com.finsight.web.restful.common;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;

import com.finsight.core.AppServiceException;
import com.finsight.core.StringTool;
import com.finsight.domain.model.Base;
import com.finsight.domain.model.User;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.CollectionResults;
import com.finsight.web.restful.model.CommonResult;
import org.slf4j.Logger;

import java.util.concurrent.Callable;

/**
 * Created by Summer.Xia on 2015/10/8.
 */
public class ControllerHelper {
	@Autowired
    HttpSession session;
	
	public User getSessionUser() throws AppServiceException{
		User user = null;
		if(session != null){
			user = new User();
			String app_username = (String)session.getAttribute("app_username");
			user.setUserName(app_username);
		}else{
			throw new AppServiceException("you are not valid user!");
		}
		return user;
	}

	protected static final String OPERATION_OK = "操作成功.";

	protected final CommonResult runCommon(Logger logger, String logMessage, AppServiceCallable<CommonResult> action) {
		try {
			return action.call();
		} catch (AppServiceException e) {
			logger.error("{}: {}", logMessage, e.getMessage(), e);
			return CommonResult.fail(e.getMessage());
		}
	}

	/**
	 * Like {@link #runCommon} but catches any {@link Exception} (e.g. mapper/runtime), matching legacy controller behavior.
	 */
	protected final CommonResult runCommonAll(Logger logger, String logMessage, Callable<CommonResult> action) {
		try {
			return action.call();
		} catch (Exception e) {
			logger.error("{}: {}", logMessage, e.getMessage(), e);
			return CommonResult.fail(e.getMessage());
		}
	}

	protected final <T> CollectionResult<T> runCollection(Logger logger, String logMessage,
			AppServiceCallable<CollectionResult<T>> action) {
		try {
			return action.call();
		} catch (AppServiceException e) {
			logger.error("{}: {}", logMessage, e.getMessage(), e);
			return CollectionResults.empty();
		} catch (Exception e) {
			logger.error("{}: {}", logMessage, e.getMessage(), e);
			return CollectionResults.empty();
		}
	}

	/** New row: id + audit fields for create. */
	protected final void stampNewRecord(Base entity, String userName) {
		entity.setId(StringTool.generateID());
		entity.setCreateuser(userName);
		entity.setUpdateuser(userName);
	}
}
