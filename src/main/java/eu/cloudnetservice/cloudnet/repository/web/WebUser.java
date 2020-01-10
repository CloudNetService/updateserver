package eu.cloudnetservice.cloudnet.repository.web;

public class WebUser {
    private String username;
    private String hashedPassword;
    private WebPermissionRole permissionRole;

    public WebUser(String username, String hashedPassword, WebPermissionRole permissionRole) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.permissionRole = permissionRole;
    }

    public String getUsername() {
        return this.username;
    }

    public String getHashedPassword() {
        return this.hashedPassword;
    }

    public WebPermissionRole getPermissionRole() {
        return this.permissionRole;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public void setPermissionRole(WebPermissionRole permissionRole) {
        this.permissionRole = permissionRole;
    }
}
