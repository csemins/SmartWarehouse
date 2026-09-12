package bd.ac.kuet.swms.model;

import java.time.LocalDate;

public class FinancialTransaction extends BaseEntity {
    public enum Type { INCOME, EXPENSE }

    private Type type;
    private String category;
    private double amount;
    private String description;
    private LocalDate date = LocalDate.now();
    private Integer movementId;
    private String username;

    public FinancialTransaction() { }

    public FinancialTransaction(Type type, String category, double amount, String description) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.description = description;
    }

    @Override
    public void validate() throws ValidationException {
        require(type != null, "Type is required");
        require(hasText(category), "Category is required");
        require(amount > 0, "Amount must be greater than 0");
        require(date != null, "Date is required");
    }

    @Override public String getDisplayName() { return type + " " + amount; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Integer getMovementId() { return movementId; }
    public void setMovementId(Integer movementId) { this.movementId = movementId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
