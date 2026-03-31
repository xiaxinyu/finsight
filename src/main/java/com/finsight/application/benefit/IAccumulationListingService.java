package com.finsight.application.benefit;

import com.finsight.core.AppServiceException;
import com.finsight.domain.model.Accumulation;
import com.finsight.web.restful.model.AccumulationParam;
import com.finsight.web.restful.model.CollectionResult;

public interface IAccumulationListingService {

    CollectionResult<Accumulation> listAccumulations(AccumulationParam param) throws AppServiceException;
}
