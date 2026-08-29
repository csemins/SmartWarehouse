package bd.ac.kuet.swms.model;

import java.util.EnumSet;
import java.util.Set;

public class InventoryManager extends User {
    @Override public Role getRole() { return Role.INVENTORY_MANAGER; }

    @Override public Set<Permission> getPermissions() {
        return EnumSet.of(Permission.VIEW_DASHBOARD, Permission.MANAGE_PRODUCTS, Permission.OPERATE_WAREHOUSE,
                Permission.MANAGE_ORDERS, Permission.VIEW_MOVEMENTS);
    }
}
