package com.finsight.web.api.auth;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.user.UserAdminService;
import com.finsight.infrastructure.mapper.UserMapper;
import com.finsight.web.api.dto.ChangePasswordRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserAdminService userAdminService;
    private final AuthenticationFacade authenticationFacade;
    private final UserMapper userMapper;

    public AuthController(UserAdminService userAdminService,
                          AuthenticationFacade authenticationFacade,
                          UserMapper userMapper) {
        this.userAdminService = userAdminService;
        this.authenticationFacade = authenticationFacade;
        this.userMapper = userMapper;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        Map<String, String> out = new LinkedHashMap<>();
        if (token != null) {
            out.put("headerName", token.getHeaderName());
            out.put("parameterName", token.getParameterName());
            out.put("token", token.getToken());
        }
        return out;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> out = new LinkedHashMap<>();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            out.put("authenticated", false);
            return out;
        }
        out.put("authenticated", true);
        out.put("username", auth.getName());
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .collect(Collectors.toList());
        out.put("roles", roles);
        out.put("admin", roles.contains("ADMIN"));
        var user = userMapper.findByUsername(auth.getName());
        if (user != null) {
            out.put("displayName", user.getDisplayName());
        }
        return out;
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@RequestBody ChangePasswordRequest body) {
        userAdminService.changeOwnPassword(authenticationFacade.getUserName(), body);
        Map<String, String> out = new LinkedHashMap<>();
        out.put("status", "ok");
        return out;
    }
}
