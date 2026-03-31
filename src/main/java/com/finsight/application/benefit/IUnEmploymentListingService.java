package com.finsight.application.benefit;

import com.finsight.core.AppServiceException;
import com.finsight.domain.model.UnEmployment;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.UnEmploymentParam;

public interface IUnEmploymentListingService {

    CollectionResult<UnEmployment> listUnEmployments(UnEmploymentParam param) throws AppServiceException;
}
