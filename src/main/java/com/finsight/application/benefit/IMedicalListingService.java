package com.finsight.application.benefit;

import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.Medical;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.MedicalParam;

public interface IMedicalListingService {

    CollectionResult<Medical> listMedicals(MedicalParam param) throws AppServiceException;
}
