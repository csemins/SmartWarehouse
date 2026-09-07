package bd.ac.kuet.swms.model;

import java.time.LocalDateTime;

public class RackCell extends BaseEntity {
    // 9 columns x 6 levels in the automated warehouse scene
    public static final int COUNT = 54;

    public enum Status { EMPTY, INBOUND, OCCUPIED, OUTBOUND }

    private Status status = Status.EMPTY;
    private Integer productId;
    private String productName;
    private String sku;
    private int quantity;
    private LocalDateTime updatedAt;

    @Override
    public void validate() throws ValidationException {
        require(getId() >= 1 && getId() <= COUNT, "Cell must be between 1 and " + COUNT);
        require(status == Status.EMPTY || productId != null, "A used cell needs a product");
    }

    public void empty() {
        status = Status.EMPTY;
        productId = null;
        quantity = 0;
    }

    @Override public String getDisplayName() { return "Cell " + getId(); }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
