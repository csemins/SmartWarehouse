package bd.ac.kuet.swms.service;

import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.model.User;

public class Session {
    // volatile: read from worker threads too
    private volatile User user;

    public User user() { return user; }

    public String username() { return user == null ? "system" : user.getUsername(); }

    public void require(Permission permission) {
        if (user == null || !user.can(permission)) throw new BusinessException("You are not allowed to do this");
    }

    void setUser(User user) { this.user = user; }
}
