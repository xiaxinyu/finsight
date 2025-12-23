package com.finsight.core;

import com.finsight.domain.model.Role;
import com.finsight.domain.model.User;
import com.finsight.infrastructure.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DbUserDetailsService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userMapper.findByUsername(username);
        if (u == null || u.getEnabled() != null && u.getEnabled() == 0) {
            throw new UsernameNotFoundException("User not found");
        }
        List<Role> roles = new ArrayList<>();
        if (u.getId() != null) {
            roles = userMapper.findRolesByUserId(u.getId());
        }
        List<GrantedAuthority> authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode()))
                .collect(Collectors.toList());
        return org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
                .password(u.getPassword())
                .authorities(authorities)
                .disabled(u.getEnabled() != null && u.getEnabled() == 0)
                .build();
    }
}
