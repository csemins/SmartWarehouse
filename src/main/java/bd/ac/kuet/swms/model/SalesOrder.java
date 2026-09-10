package bd.ac.kuet.swms.model;

public class SalesOrder extends BaseEntity {
    public enum Status { OPEN, DONE, CANCELLED }

    private String customer;
    private int productId;
    private String productName;
    private int boxes;
    private int boxesShipped;
    private Status status = Status.OPEN;
    private String username;

    @Override
    public void validate() throws ValidationException {
        require(hasText(customer), "Customer is required");
        require(productId > 0, "Product is required");
        require(boxes > 0, "Order at least one box");
    }

    public int boxesLeft() { return boxes - boxesShipped; }

    @Override public String getDisplayName() { return "Order #" + getId() + " " + customer; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getBoxes() { return boxes; }
    public void setBoxes(int boxes) { this.boxes = boxes; }
    public int getBoxesShipped() { return boxesShipped; }
    public void setBoxesShipped(int boxesShipped) { this.boxesShipped = boxesShipped; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
