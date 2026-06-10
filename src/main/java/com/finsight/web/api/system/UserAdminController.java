package com.finsight.web.api.system;

import com.finsight.infrastructure.mapper.UserMapper;
import com.finsight.infrastructure.mapper.RoleMapper;
import com.finsight.domain.model.User;
import com.finsight.domain.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserAdminController {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public List<User> list(){
        return userMapper.listAll();
    }

    @PostMapping
    public User create(@RequestBody User user){
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()){
            user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
        }
        if (user.getEnabled() == null){
            user.setEnabled(1);
        }
        userMapper.insert(user);
        return userMapper.findById(user.getId());
    }

    @PutMapping("/{id}")
    public User update(@PathVariable("id") Long id, @RequestBody User user){
        user.setId(id);
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()){
            user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
        }
        userMapper.update(user);
        return userMapper.findById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id){
        userMapper.delete(id);
    }

    @GetMapping("/{id}/roles")
    public List<Role> getUserRoles(@PathVariable("id") Long id){
        return userMapper.findRolesByUserId(id);
    }

    @GetMapping("/roles")
    public List<Role> allRoles(){
        return roleMapper.findAll();
    }

    @PostMapping("/{id}/roles")
    public void setRoles(@PathVariable("id") Long id, @RequestBody List<Long> roleIds){
        userMapper.deleteRolesByUserId(id);
        if (roleIds != null){
            for (Long rid : roleIds){
                userMapper.addUserRole(id, rid);
            }
        }
    }
}
