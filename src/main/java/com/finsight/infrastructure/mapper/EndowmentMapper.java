package com.finsight.infrastructure.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import com.finsight.domain.model.Endowment;
import com.finsight.domain.model.Page;

public interface EndowmentMapper extends BaseMapper<Endowment> {
    void addEndowment(Endowment endowment);
    void updateEndowment(Endowment endowment);
    void deleteEndowment(String id);
    int countEndowments(@Param("endowment") Endowment endowment);
    List<Endowment> getEndowments(@Param("endowment") Endowment endowment, @Param("page") Page page);
}
