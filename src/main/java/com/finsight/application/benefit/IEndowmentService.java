package com.finsight.application.benefit;

import java.util.List;

import com.finsight.domain.model.Endowment;
import com.finsight.domain.model.Page;
import com.finsight.common.exception.AppServiceException;

public interface IEndowmentService {
    void addEndowment(Endowment endowment) throws AppServiceException;

    void updateEndowment(Endowment endowment) throws AppServiceException;

    void deleteEndowment(String id) throws AppServiceException;

    int countEndowments(Endowment endowment) throws AppServiceException;

    List<Endowment> getEndowments(Endowment endowment, Page page) throws AppServiceException;
}
