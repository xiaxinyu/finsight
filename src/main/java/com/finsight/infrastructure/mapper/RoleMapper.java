package com.finsight.infrastructure.mapper;

import org.apache.ibatis.annotations.Param;

import com.finsight.domain.model.Role;

import java.util.List;

public interface RoleMapper {
    Role findByCode(@Param("code") String code);
    List<Role> findAll();
}
