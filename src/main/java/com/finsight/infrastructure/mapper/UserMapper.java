package com.finsight.infrastructure.mapper;

import org.apache.ibatis.annotations.Param;

import com.finsight.domain.model.Role;
import com.finsight.domain.model.User;

import java.util.List;

public interface UserMapper {
    User findByUsername(@Param("username") String username);
    User findById(@Param("id") Long id);
    List<User> listAll();
    int insert(User user);
    int update(User user);
    int delete(@Param("id") Long id);
    List<Role> findRolesByUserId(@Param("userId") Long userId);
    int deleteRolesByUserId(@Param("userId") Long userId);
    int addUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
