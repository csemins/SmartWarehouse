package bd.ac.kuet.swms.service;

import bd.ac.kuet.swms.model.AuditEntry;
import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.model.SalesOrder;
import bd.ac.kuet.swms.repository.SalesOrderRepository;

import java.util.List;
import java.util.Map;

public class SalesOrderService {
    private final SalesOrderRepository orders;
    private final Session session;
    private final AuditService audit;

    public SalesOrderService(SalesOrderRepository orders, Session session, AuditService audit) {
        this.audit = audit;
        this.orders = orders;
        this.session = session;
    }

    public List<SalesOrder> findAll() { return orders.findAll(); }

    public List<SalesOrder> openOrders() { return orders.openOrders(); }

    public Map<SalesOrder.Status, Integer> countByStatus() { return orders.countByStatus(); }

    public SalesOrder create(String customer, Product product, int boxes) {
        session.require(Permission.MANAGE_ORDERS);
        SalesOrder order = new SalesOrder();
        order.setCustomer(customer.trim());
        order.setProductId(product == null ? 0 : product.getId());
        order.setBoxes(boxes);
        order.setUsername(session.username());
        SalesOrder saved = orders.save(order);
        audit.record(AuditEntry.Category.ORDERS, "Created order #" + saved.getId() + " for " + saved.getCustomer() + " (" + boxes + " box(es) of " + product.getName() + ")");
        return saved;
    }

    public void cancel(SalesOrder order) {
        session.require(Permission.MANAGE_ORDERS);
        if (order.getStatus() != SalesOrder.Status.OPEN) throw new BusinessException("Only open orders can be cancelled");
        order.setStatus(SalesOrder.Status.CANCELLED);
        orders.save(order);
        audit.record(AuditEntry.Category.ORDERS, "Cancelled order #" + order.getId());
    }

    public void delete(SalesOrder order) {
        session.require(Permission.MANAGE_ORDERS);
        if (order.getBoxesShipped() > 0) throw new BusinessException("Boxes of this order were already shipped; cancel it instead");
        orders.delete(order.getId());
        audit.record(AuditEntry.Category.ORDERS, "Deleted order #" + order.getId());
    }

    // called by WarehouseService after an OUT booking
    void recordShippedBox(int orderId) {
        orders.findById(orderId).ifPresent(order -> {
            order.setBoxesShipped(order.getBoxesShipped() + 1);
            if (order.boxesLeft() <= 0) order.setStatus(SalesOrder.Status.DONE);
            orders.save(order);
        });
    }
}
