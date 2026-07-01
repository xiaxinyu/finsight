package com.finsight.web.api.system;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.user.UserAdminService;
import com.finsight.domain.model.Role;
import com.finsight.web.api.dto.ChangePasswordRequest;
import com.finsight.web.api.dto.ResetPasswordRequest;
import com.finsight.web.api.dto.UserAdminDto;
import com.finsight.web.api.dto.UserWriteRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final AuthenticationFacade authenticationFacade;

    public UserAdminController(UserAdminService userAdminService, AuthenticationFacade authenticationFacade) {
        this.userAdminService = userAdminService;
        this.authenticationFacade = authenticationFacade;
    }

    @GetMapping
    public List<UserAdminDto> list(@RequestParam(value = "q", required = false) String q) {
        return userAdminService.list(q);
    }

    @PostMapping
    public UserAdminDto create(@RequestBody UserWriteRequest body) {
        return userAdminService.create(body, authenticationFacade.getUserName());
    }

    @PutMapping("/{id}")
    public UserAdminDto update(@PathVariable("id") Long id, @RequestBody UserWriteRequest body) {
        return userAdminService.update(id, body, authenticationFacade.getUserName());
    }

    @PostMapping("/{id}/reset-password")
    public Map<String, String> resetPassword(@PathVariable("id") Long id, @RequestBody ResetPasswordRequest body) {
        userAdminService.resetPassword(id, body, authenticationFacade.getUserName());
        Map<String, String> out = new LinkedHashMap<>();
        out.put("status", "ok");
        return out;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        userAdminService.delete(id, authenticationFacade.getUserName());
    }

    @GetMapping("/roles")
    public List<Role> allRoles() {
        return userAdminService.allRoles();
    }

    @GetMapping("/{id}/roles")
    public List<Role> getUserRoles(@PathVariable("id") Long id) {
        return userAdminService.rolesForUser(id);
    }

    /** Legacy role assignment endpoint — prefer roleIds on create/update. */
    @PostMapping("/{id}/roles")
    public void setRoles(@PathVariable("id") Long id, @RequestBody List<Long> roleIds) {
        UserWriteRequest req = new UserWriteRequest();
        req.setRoleIds(roleIds);
        userAdminService.update(id, req, authenticationFacade.getUserName());
    }
}
