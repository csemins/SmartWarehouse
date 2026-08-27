package bd.ac.kuet.swms.model;

import java.util.EnumSet;
import java.util.Set;

public class Admin extends User {
    @Override public Role getRole() { return Role.ADMIN; }

    @Override public Set<Permission> getPermissions() { return EnumSet.allOf(Permission.class); }
}
