package com.finsight.application.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.finsight.core.DataStructureTool;
import com.finsight.core.StringTool;
import com.finsight.infrastructure.mapper.AccumulationMapper;
import com.finsight.domain.model.Accumulation;
import com.finsight.domain.model.Page;
import com.finsight.core.AppServiceException;
import com.finsight.application.IAccumulationService;

/**
 * Created by Summer.Xia on 10/13/2015.
 */
@Service
public class AccumulationServiceImpl implements IAccumulationService {
    @Autowired
    AccumulationMapper accumulationMapper;

    @Override
    public void addAccumulation(Accumulation accumulation) throws AppServiceException {
        try {
            accumulationMapper.addAccumulation(accumulation);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
    }

    @Override
    public void updateAccumulation(Accumulation accumulation) throws AppServiceException {
        try {
            accumulationMapper.updateAccumulation(accumulation);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
    }

    @Override
    public void deleteAccumulation(String id) throws AppServiceException {
        try {
            accumulationMapper.deleteAccumulation(id);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
    }

    @Override
    public int countAccumulations(Accumulation accumulation) throws AppServiceException {
        int result = 0;
        try {
            result = accumulationMapper.countAccumulations(accumulation);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }

    @Override
    public List<Accumulation> getAccumulations(Accumulation accumulation, Page page) throws AppServiceException {
        List<Accumulation> result = null;
        try {
            result = accumulationMapper.getAccumulations(accumulation, page);
            if (DataStructureTool.isNotEmpty(result)) {
                for (Accumulation item : result) {
                    String time = item.getTime();
                    item.setYear(time);
                    if (!StringTool.isNullOrEmpty(time)) {
                        String[] args = time.split("-");
                        if (args.length >= 2) {
                            item.setYear(args[0]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }
}
