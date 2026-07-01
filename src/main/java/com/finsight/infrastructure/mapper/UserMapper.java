package com.finsight.infrastructure.mapper;

import org.apache.ibatis.annotations.Param;

import com.finsight.domain.model.Role;
import com.finsight.domain.model.User;

import java.util.List;

public interface UserMapper {
    User findByUsername(@Param("username") String username);
    User findById(@Param("id") Long id);
    List<User> listAll();
    List<User> search(@Param("q") String q);
    int insert(@Param("user") User user, @Param("actor") String actor);
    int update(@Param("user") User user, @Param("actor") String actor);
    int updatePassword(@Param("id") Long id, @Param("password") String password, @Param("actor") String actor);
    int delete(@Param("id") Long id);
    int countEnabledAdmins();
    List<Role> findRolesByUserId(@Param("userId") Long userId);
    int deleteRolesByUserId(@Param("userId") Long userId);
    int addUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
