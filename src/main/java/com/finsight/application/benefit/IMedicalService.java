package com.finsight.application.benefit;

import java.util.List;

import com.finsight.domain.model.Medical;
import com.finsight.domain.model.Page;
import com.finsight.core.AppServiceException;

public interface IMedicalService {
    void addMedical(Medical medical) throws AppServiceException;

    void updateMedical(Medical medical) throws AppServiceException;

    void deleteMedical(String id) throws AppServiceException;

    int countMedicals(Medical medical) throws AppServiceException;

    List<Medical> getMedicals(Medical medical, Page page) throws AppServiceException;
}
