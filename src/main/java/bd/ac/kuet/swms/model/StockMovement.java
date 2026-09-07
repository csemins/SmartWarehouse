package bd.ac.kuet.swms.model;

public class StockMovement extends BaseEntity {
    public enum Type { IN, OUT }

    private int productId;
    private int cellId;
    private Integer orderId;
    private Type type;
    private int quantity;
    private String username;
    private String productName;

    public StockMovement() { }

    public StockMovement(int productId, int cellId, Integer orderId, Type type, int quantity, String username) {
        this.productId = productId;
        this.cellId = cellId;
        this.orderId = orderId;
        this.type = type;
        this.quantity = quantity;
        this.username = username;
    }

    @Override
    public void validate() throws ValidationException {
        require(productId > 0, "Product is required");
        require(cellId >= 1 && cellId <= RackCell.COUNT, "Invalid cell");
        require(quantity > 0, "Quantity must be positive");
        require(type != null, "Type is required");
    }

    @Override public String getDisplayName() { return type + " " + quantity + " x " + productName; }

    public int getProductId() { return productId; }
    public int getCellId() { return cellId; }
    public Integer getOrderId() { return orderId; }
    public Type getType() { return type; }
    public int getQuantity() { return quantity; }
    public String getUsername() { return username; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
