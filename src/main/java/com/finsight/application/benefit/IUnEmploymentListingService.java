package com.finsight.application.benefit;

import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.UnEmployment;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.UnEmploymentParam;

public interface IUnEmploymentListingService {

    CollectionResult<UnEmployment> listUnEmployments(UnEmploymentParam param) throws AppServiceException;
}
