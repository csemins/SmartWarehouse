package bd.ac.kuet.swms.model;

import java.util.Set;

public abstract class User extends BaseEntity {
    private String username;
    private String fullName;
    private String salt;
    private String passwordHash;
    private boolean active = true;

    public abstract Role getRole();

    // each role decides what it can see
    public abstract Set<Permission> getPermissions();

    public boolean can(Permission permission) { return active && getPermissions().contains(permission); }

    @Override
    public void validate() throws ValidationException {
        require(hasText(username) && username.matches("[A-Za-z0-9_.]{3,32}"), "Username must be 3-32 letters, digits, _ or .");
        require(hasText(fullName), "Full name is required");
    }

    @Override public String getDisplayName() { return fullName + " (" + username + ")"; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
