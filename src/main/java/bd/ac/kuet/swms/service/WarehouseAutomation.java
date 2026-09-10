package bd.ac.kuet.swms.service;

import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.model.SalesOrder;
import bd.ac.kuet.swms.plc.ControllerState;
import bd.ac.kuet.swms.plc.Sensor;
import bd.ac.kuet.swms.plc.Snapshot;
import bd.ac.kuet.swms.util.AppExecutors;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WarehouseAutomation {
    private final WarehouseService warehouse;
    private final SalesOrderService orders;
    private final Session session;
    private volatile Product autoStoreProduct;
    private volatile Integer autoStoreCell;
    private volatile String lastMessage = "";
    private ScheduledFuture<?> task;

    public WarehouseAutomation(WarehouseService warehouse, SalesOrderService orders, Session session) {
        this.warehouse = warehouse;
        this.orders = orders;
        this.session = session;
    }

    public void start() {
        if (task == null) task = AppExecutors.scheduler().scheduleWithFixedDelay(this::runOnce, 1, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        if (task != null) task.cancel(false);
        task = null;
    }

    public void setAutoStoreProduct(Product product) {
        setAutoStore(product, null);
    }

    public void setAutoStore(Product product, Integer preferredCell) {
        session.require(Permission.OPERATE_WAREHOUSE);
        autoStoreProduct = product;
        autoStoreCell = preferredCell;
    }

    public Product autoStoreProduct() { return autoStoreProduct; }

    public String lastMessage() { return lastMessage; }

    // runs every second. only acts when the crane is idle and nothing is queued
    void runOnce() {
        try {
            Snapshot s = warehouse.controller().snapshot();
            if (s == null || s.state() != ControllerState.RUNNING || s.currentJob() != null || !s.queue().isEmpty() || warehouse.isBooking()) return;
            if (autoStoreProduct != null && boxIsWaiting(s)) {
                Integer cell = autoStoreCell != null && warehouse.isEmpty(autoStoreCell) ? autoStoreCell : null;
                var job = warehouse.store(autoStoreProduct, cell, "automation");
                lastMessage = "Automatic store of " + autoStoreProduct.getName() + " into cell " + job.getCell();
                return;
            }
            for (SalesOrder order : orders.openOrders()) {
                var cell = warehouse.oldestBoxOf(order.getProductId());
                if (cell.isPresent()) {
                    warehouse.retrieve(cell.get().getId(), order.getId(), order.getUsername());
                    lastMessage = "Shipping a box for order #" + order.getId();
                    return;
                }
            }
        } catch (RuntimeException e) {
            lastMessage = "Automation paused: " + e.getMessage();
        }
    }

    private boolean boxIsWaiting(Snapshot s) {
        boolean inverted = warehouse.settings().conveyorSensorsInverted;
        return s.input(Sensor.AT_ENTRY) != inverted || s.input(Sensor.AT_LOAD) != inverted;
    }
}
