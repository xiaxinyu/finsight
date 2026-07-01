package com.finsight.web.api.dto;

import java.util.List;

public class UserWriteRequest {
    private String username;
    private String password;
    private String displayName;
    private Integer enabled;
    private List<Long> roleIds;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
}
