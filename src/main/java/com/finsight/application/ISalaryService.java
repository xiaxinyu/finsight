package com.finsight.application;

import java.util.List;

import com.finsight.domain.model.Page;
import com.finsight.domain.model.Salary;
import com.finsight.core.AppServiceException;

/**
 * Created by Summer.Xia on 12/12/2018.
 */
public interface ISalaryService {
    int countSalary(Salary salary) throws AppServiceException;

    List<Salary> getSalarys(Salary salary, Page page) throws AppServiceException;
}
