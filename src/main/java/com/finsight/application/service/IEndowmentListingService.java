package com.finsight.application.service;

import com.finsight.core.AppServiceException;
import com.finsight.domain.model.Endowment;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.EndowmentParam;

public interface IEndowmentListingService {

    CollectionResult<Endowment> listEndowments(EndowmentParam param) throws AppServiceException;
}
