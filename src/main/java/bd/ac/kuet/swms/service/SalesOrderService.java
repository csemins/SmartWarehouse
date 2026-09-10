package bd.ac.kuet.swms.service;

import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.model.SalesOrder;
import bd.ac.kuet.swms.repository.SalesOrderRepository;

import java.util.List;

public class SalesOrderService {
    private final SalesOrderRepository orders;
    private final Session session;

    public SalesOrderService(SalesOrderRepository orders, Session session) {
        this.orders = orders;
        this.session = session;
    }

    public List<SalesOrder> findAll() { return orders.findAll(); }

    public List<SalesOrder> openOrders() { return orders.openOrders(); }

    public SalesOrder create(String customer, Product product, int boxes) {
        session.require(Permission.MANAGE_ORDERS);
        SalesOrder order = new SalesOrder();
        order.setCustomer(customer.trim());
        order.setProductId(product == null ? 0 : product.getId());
        order.setBoxes(boxes);
        order.setUsername(session.username());
        SalesOrder saved = orders.save(order);
        return saved;
    }

    public void cancel(SalesOrder order) {
        session.require(Permission.MANAGE_ORDERS);
        if (order.getStatus() != SalesOrder.Status.OPEN) throw new BusinessException("Only open orders can be cancelled");
        order.setStatus(SalesOrder.Status.CANCELLED);
        orders.save(order);
    }

    public void delete(SalesOrder order) {
        session.require(Permission.MANAGE_ORDERS);
        if (order.getBoxesShipped() > 0) throw new BusinessException("Boxes of this order were already shipped; cancel it instead");
        orders.delete(order.getId());
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
