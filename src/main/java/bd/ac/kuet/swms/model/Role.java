package bd.ac.kuet.swms.model;

public enum Role {
    ADMIN("Administrator"), INVENTORY_MANAGER("Inventory Manager"), FINANCE_MANAGER("Finance Manager");

    private final String label;

    Role(String label) { this.label = label; }

    @Override public String toString() { return label; }
}
