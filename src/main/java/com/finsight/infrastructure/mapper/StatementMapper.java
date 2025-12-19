package com.finsight.infrastructure.mapper;

import com.finsight.domain.model.Statement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatementMapper extends BaseMapper<Statement> {
}
