package com.finsight.application.benefit;

import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.Endowment;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.EndowmentParam;

public interface IEndowmentListingService {

    CollectionResult<Endowment> listEndowments(EndowmentParam param) throws AppServiceException;
}
