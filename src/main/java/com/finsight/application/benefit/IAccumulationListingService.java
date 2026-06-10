package com.finsight.application.benefit;

import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.Accumulation;
import com.finsight.web.api.dto.AccumulationParam;
import com.finsight.web.api.dto.CollectionResult;

public interface IAccumulationListingService {

    CollectionResult<Accumulation> listAccumulations(AccumulationParam param) throws AppServiceException;
}
