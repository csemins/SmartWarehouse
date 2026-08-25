package bd.ac.kuet.swms.model;

public class Product extends BaseEntity {
    private String sku;
    private String name;
    private Integer categoryId;
    private String categoryName;
    private double costPrice;
    private double unitPrice;
    private int reorderLevel;
    private int quantity;

    @Override
    public void validate() throws ValidationException {
        require(hasText(sku), "SKU / barcode is required");
        require(hasText(name), "Name is required");
        require(costPrice >= 0 && unitPrice >= 0, "Prices cannot be negative");
        require(reorderLevel >= 0, "Reorder level cannot be negative");
    }

    // reorder level 0 = don't care
    public boolean needsRestock() { return reorderLevel > 0 && quantity <= reorderLevel; }

    public double getStockValue() { return quantity * costPrice; }

    @Override public String getDisplayName() { return name + " (" + sku + ")"; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
