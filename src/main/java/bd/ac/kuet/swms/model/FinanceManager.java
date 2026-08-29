package bd.ac.kuet.swms.model;

import java.util.EnumSet;
import java.util.Set;

public class FinanceManager extends User {
    @Override public Role getRole() { return Role.FINANCE_MANAGER; }

    @Override public Set<Permission> getPermissions() {
        return EnumSet.of(Permission.VIEW_DASHBOARD, Permission.OPERATE_WAREHOUSE, Permission.MANAGE_ORDERS, Permission.VIEW_MOVEMENTS, Permission.MANAGE_FINANCE);
    }
}
