package com.finsight.application.user;

import com.finsight.common.exception.AppException;
import com.finsight.common.security.PasswordPolicy;
import com.finsight.common.security.SecurityRoles;
import com.finsight.domain.model.Role;
import com.finsight.domain.model.User;
import com.finsight.infrastructure.mapper.RoleMapper;
import com.finsight.infrastructure.mapper.UserMapper;
import com.finsight.web.api.dto.ChangePasswordRequest;
import com.finsight.web.api.dto.ResetPasswordRequest;
import com.finsight.web.api.dto.UserAdminDto;
import com.finsight.web.api.dto.UserWriteRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UserMapper userMapper, RoleMapper roleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserAdminDto> list(String query) {
        String q = StringUtils.trimToNull(query);
        List<User> users = q == null ? userMapper.listAll() : userMapper.search(q);
        List<UserAdminDto> out = new ArrayList<>();
        for (User user : users) {
            out.add(toDto(user));
        }
        return out;
    }

    @Transactional
    public UserAdminDto create(UserWriteRequest req, String actor) {
        validateUsername(req.getUsername());
        PasswordPolicy.validate(req.getPassword());

        if (userMapper.findByUsername(req.getUsername().trim()) != null) {
            throw new AppException("Username already exists");
        }

        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setDisplayName(StringUtils.trimToNull(req.getDisplayName()));
        user.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        user.setPassword(passwordEncoder.encode(req.getPassword().trim()));
        userMapper.insert(user, actor);

        List<Long> roleIds = resolveRoleIds(req.getRoleIds(), defaultUserRoleId());
        replaceRoles(user.getId(), roleIds);
        return toDto(userMapper.findById(user.getId()));
    }

    @Transactional
    public UserAdminDto update(Long id, UserWriteRequest req, String actor) {
        User existing = requireUser(id);
        User user = new User();
        user.setId(id);
        user.setUsername(existing.getUsername());
        user.setDisplayName(req.getDisplayName() != null
                ? StringUtils.trimToNull(req.getDisplayName())
                : existing.getDisplayName());
        user.setEnabled(req.getEnabled() != null ? req.getEnabled() : existing.getEnabled());
        user.setPassword(existing.getPassword());
        userMapper.update(user, actor);

        if (req.getRoleIds() != null) {
            assertAdminSurvival(id, req.getRoleIds(), existing.getEnabled(), user.getEnabled());
            replaceRoles(id, req.getRoleIds());
        }
        return toDto(userMapper.findById(id));
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest req, String actor) {
        requireUser(id);
        PasswordPolicy.validate(req.getPassword());
        User patch = new User();
        patch.setId(id);
        patch.setPassword(passwordEncoder.encode(req.getPassword().trim()));
        userMapper.updatePassword(id, patch.getPassword(), actor);
    }

    @Transactional
    public void changeOwnPassword(String username, ChangePasswordRequest req) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new AppException("User not found");
        }
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new AppException("Current password is incorrect");
        }
        if (Objects.equals(req.getCurrentPassword(), req.getNewPassword())) {
            throw new AppException("New password must differ from current password");
        }
        PasswordPolicy.validate(req.getNewPassword());
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(req.getNewPassword().trim()), username);
    }

    @Transactional
    public void delete(Long id, String actorUsername) {
        User user = requireUser(id);
        if (actorUsername != null && actorUsername.equalsIgnoreCase(user.getUsername())) {
            throw new AppException("You cannot delete your own account");
        }
        if (hasAdminRole(id) && userMapper.countEnabledAdmins() <= 1) {
            throw new AppException("Cannot delete the last active administrator");
        }
        userMapper.deleteRolesByUserId(id);
        userMapper.delete(id);
    }

    public List<Role> allRoles() {
        return roleMapper.findAll();
    }

    public List<Role> rolesForUser(Long userId) {
        return userMapper.findRolesByUserId(userId);
    }

    private User requireUser(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new AppException("User not found");
        }
        return user;
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userMapper.deleteRolesByUserId(userId);
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleId != null) {
                userMapper.addUserRole(userId, roleId);
            }
        }
    }

    private List<Long> resolveRoleIds(List<Long> requested, Long defaultRoleId) {
        if (requested != null && !requested.isEmpty()) {
            return requested;
        }
        return defaultRoleId == null ? List.of() : List.of(defaultRoleId);
    }

    private Long defaultUserRoleId() {
        return roleMapper.findAll().stream()
                .filter(r -> SecurityRoles.USER.equals(r.getCode()))
                .map(Role::getId)
                .findFirst()
                .orElse(null);
    }

    private void assertAdminSurvival(Long userId, List<Long> newRoleIds, Integer oldEnabled, Integer newEnabled) {
        if (!hasAdminRole(userId)) {
            return;
        }
        boolean stillAdmin = roleListHasAdmin(newRoleIds);
        boolean disabling = (oldEnabled == null || oldEnabled == 1) && newEnabled != null && newEnabled == 0;
        if ((!stillAdmin || disabling) && userMapper.countEnabledAdmins() <= 1) {
            throw new AppException("Cannot remove the last active administrator");
        }
    }

    private boolean roleListHasAdmin(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        List<Role> all = roleMapper.findAll();
        for (Long rid : roleIds) {
            if (rid == null) {
                continue;
            }
            for (Role r : all) {
                if (rid.equals(r.getId()) && SecurityRoles.ADMIN.equals(r.getCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAdminRole(Long userId) {
        return userMapper.findRolesByUserId(userId).stream()
                .anyMatch(r -> SecurityRoles.ADMIN.equals(r.getCode()));
    }

    private static void validateUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new AppException("Username is required");
        }
        String u = username.trim();
        if (u.length() < 2 || u.length() > 64) {
            throw new AppException("Username must be 2–64 characters");
        }
        if (!u.matches("[A-Za-z0-9._-]+")) {
            throw new AppException("Username may only contain letters, digits, ., _, -");
        }
    }

    private UserAdminDto toDto(User user) {
        UserAdminDto dto = new UserAdminDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setEnabled(user.getEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setRoles(userMapper.findRolesByUserId(user.getId()).stream()
                .map(Role::getCode)
                .collect(Collectors.toList()));
        return dto;
    }
}
