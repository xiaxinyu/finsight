package com.finsight.application.service;

import java.util.List;

import com.finsight.domain.model.Page;
import com.finsight.domain.model.Salary;
import com.finsight.core.AppServiceException;

public interface ISalaryService {
    int countSalary(Salary salary) throws AppServiceException;

    List<Salary> getSalarys(Salary salary, Page page) throws AppServiceException;
}
