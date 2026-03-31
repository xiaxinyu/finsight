package com.finsight.application.benefit;

import java.util.List;

import com.finsight.domain.model.Accumulation;
import com.finsight.domain.model.Page;
import com.finsight.core.AppServiceException;

public interface IAccumulationService {
    void addAccumulation(Accumulation accumulation) throws AppServiceException;

    void updateAccumulation(Accumulation accumulation) throws AppServiceException;

    void deleteAccumulation(String id) throws AppServiceException;

    int countAccumulations(Accumulation accumulation) throws AppServiceException;

    List<Accumulation> getAccumulations(Accumulation accumulation, Page page) throws AppServiceException;
}
