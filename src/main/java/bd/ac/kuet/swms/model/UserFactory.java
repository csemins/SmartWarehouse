package bd.ac.kuet.swms.model;

public final class UserFactory {
    private UserFactory() { }

    public static User create(Role role) {
        return switch (role) {
            case ADMIN -> new Admin();
            case INVENTORY_MANAGER -> new InventoryManager();
            case FINANCE_MANAGER -> new FinanceManager();
        };
    }
}
