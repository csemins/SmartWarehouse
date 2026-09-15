package bd.ac.kuet.swms.model;

public class AuditEntry extends BaseEntity {
    public enum Category { SECURITY, PRODUCTS, ORDERS, WAREHOUSE, FINANCE, SYSTEM }

    private Integer userId;
    private String username;
    private Category category;
    private String action;

    public AuditEntry() { }

    public AuditEntry(Integer userId, String username, Category category, String action) {
        this.userId = userId;
        this.username = username;
        this.category = category;
        this.action = action;
    }

    @Override
    public void validate() throws ValidationException {
        require(hasText(username), "User is required");
        require(category != null, "Category is required");
        require(hasText(action), "Action is required");
    }

    @Override public String getDisplayName() { return action; }

    public Integer getUserId() { return userId; }
    public String getUsername() { return username; }
    public Category getCategory() { return category; }
    public String getAction() { return action; }
}
