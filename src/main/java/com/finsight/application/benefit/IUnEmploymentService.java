package com.finsight.application.benefit;

import java.util.List;

import com.finsight.domain.model.Page;
import com.finsight.domain.model.UnEmployment;
import com.finsight.core.AppServiceException;

public interface IUnEmploymentService {
    void addUnEmployment(UnEmployment unEmployment) throws AppServiceException;

    void updateUnEmployment(UnEmployment unEmployment) throws AppServiceException;

    void deleteUnEmployment(String id) throws AppServiceException;

    int countUnEmployments(UnEmployment unEmployment) throws AppServiceException;

    List<UnEmployment> getUnEmployments(UnEmployment unEmployment, Page page) throws AppServiceException;
}
