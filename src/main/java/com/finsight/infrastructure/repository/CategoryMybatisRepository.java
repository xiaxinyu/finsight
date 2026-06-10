package com.finsight.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.port.CategoryRepository;
import com.finsight.infrastructure.mapper.ConsumeCategoryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CategoryMybatisRepository implements CategoryRepository {

    private final ConsumeCategoryMapper mapper;

    public CategoryMybatisRepository(ConsumeCategoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ConsumeCategory findById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public ConsumeCategory findByCode(String code) {
        QueryWrapper<ConsumeCategory> qw = new QueryWrapper<>();
        qw.eq("code", code).ne("deleted", 1).last("limit 1");
        return mapper.selectOne(qw);
    }

    @Override
    public List<ConsumeCategory> listActive() {
        QueryWrapper<ConsumeCategory> qw = new QueryWrapper<>();
        qw.ne("deleted", 1).orderByAsc("sort_no");
        return mapper.selectList(qw);
    }
}
