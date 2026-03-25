package com.finsight.application.service;

import com.finsight.core.AppServiceException;
import com.finsight.domain.model.Medical;
import com.finsight.web.restful.model.CollectionResult;
import com.finsight.web.restful.model.MedicalParam;

public interface IMedicalListingService {

    CollectionResult<Medical> listMedicals(MedicalParam param) throws AppServiceException;
}
