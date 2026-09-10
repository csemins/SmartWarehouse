package bd.ac.kuet.swms.service;

import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.model.Product;
import bd.ac.kuet.swms.model.RackCell;
import bd.ac.kuet.swms.model.StockMovement;
import bd.ac.kuet.swms.plc.FactoryIoController;
import bd.ac.kuet.swms.plc.JobListener;
import bd.ac.kuet.swms.plc.ModbusTcpDriver;
import bd.ac.kuet.swms.plc.WarehouseJob;
import bd.ac.kuet.swms.repository.ProductRepository;
import bd.ac.kuet.swms.repository.RackCellRepository;
import bd.ac.kuet.swms.repository.StockMovementRepository;
import bd.ac.kuet.swms.util.AppExecutors;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class WarehouseService implements JobListener {
    private final Database db;
    private final RackCellRepository cells;
    private final ProductRepository products;
    private final StockMovementRepository movements;
    private final SalesOrderService orders;
    private final Session session;
    private final Path settingsFile;
    private final FactoryIoController controller;
    private final AtomicInteger pendingBookings = new AtomicInteger();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> messageListeners = new CopyOnWriteArrayList<>();
    private AppSettings settings;

    public WarehouseService(Database db, RackCellRepository cells, ProductRepository products, StockMovementRepository movements,
                            SalesOrderService orders, Session session, Path settingsFile) {
        this.db = db;
        this.cells = cells;
        this.products = products;
        this.movements = movements;
        this.orders = orders;
        this.session = session;
        this.settingsFile = settingsFile;
        this.settings = AppSettings.load(settingsFile);
        this.controller = new FactoryIoController(AppExecutors.scheduler(), this);
    }

    public FactoryIoController controller() { return controller; }

    public WarehouseJob requestStore(Product product, Integer targetCell) {
        session.require(Permission.OPERATE_WAREHOUSE);
        return store(product, targetCell, session.username());
    }

    public WarehouseJob requestRetrieve(int cellId) {
        session.require(Permission.OPERATE_WAREHOUSE);
        return retrieve(cellId, null, session.username());
    }

    public WarehouseJob requestRetrieveOldest(Product product) {
        RackCell cell = oldestBoxOf(product.getId()).orElseThrow(() -> new BusinessException("No box of " + product.getName() + " in the rack"));
        return requestRetrieve(cell.getId());
    }

    // cell is reserved (INBOUND) right away so two jobs can't pick the same one
    synchronized WarehouseJob store(Product product, Integer targetCell, String username) {
        RackCell cell = chooseEmptyCell(targetCell);
        cell.setStatus(RackCell.Status.INBOUND);
        cell.setProductId(product.getId());
        cell.setQuantity(product.getUnitsPerBox());
        cells.save(cell);
        WarehouseJob job = WarehouseJob.store(settings.craneConfig(), cell.getId(), product.getName() + " x" + product.getUnitsPerBox());
        job.setUsername(username);
        controller.enqueue(job);
        notifyChanged();
        return job;
    }

    private RackCell chooseEmptyCell(Integer targetCell) {
        if (targetCell == null) return cells.firstEmpty().orElseThrow(() -> new BusinessException("The rack is full"));
        RackCell cell = cells.findById(targetCell).orElseThrow(() -> new BusinessException("Cell " + targetCell + " does not exist"));
        if (cell.getStatus() != RackCell.Status.EMPTY) throw new BusinessException("Cell " + targetCell + " is not empty");
        return cell;
    }

    public boolean isEmpty(int cellId) {
        return cells.findById(cellId).map(c -> c.getStatus() == RackCell.Status.EMPTY).orElse(false);
    }

    synchronized WarehouseJob retrieve(int cellId, Integer orderId, String username) {
        RackCell cell = cells.findById(cellId).orElseThrow();
        if (cell.getStatus() != RackCell.Status.OCCUPIED) throw new BusinessException("Cell " + cellId + " has no box to retrieve");
        cell.setStatus(RackCell.Status.OUTBOUND);
        cells.save(cell);
        WarehouseJob job = WarehouseJob.retrieve(settings.craneConfig(), cellId, cell.getProductName() + " x" + cell.getQuantity());
        job.setUsername(username);
        job.setOrderId(orderId);
        controller.enqueue(job);
        notifyChanged();
        return job;
    }

    @Override
    // called from the scan thread -> don't block it, book on a worker
    public void jobFinished(WarehouseJob job) {
        pendingBookings.incrementAndGet();
        AppExecutors.workers().submit(() -> {
            try {
                if (job.getKind() == WarehouseJob.Kind.STORE) bookStoredBox(job);
                else bookShippedBox(job);
            } finally {
                pendingBookings.decrementAndGet();
                notifyChanged();
            }
        });
    }

    @Override
    public void jobAborted(WarehouseJob job) {
        AppExecutors.workers().submit(() -> {
            RackCell cell = cells.findById(job.getCell()).orElseThrow();
            if (job.getKind() == WarehouseJob.Kind.STORE) cell.empty();
            else cell.setStatus(RackCell.Status.OCCUPIED);
            cells.save(cell);
            announce("Crane job cancelled: " + job);
            notifyChanged();
        });
    }

    void bookStoredBox(WarehouseJob job) {
        RackCell cell = cells.findById(job.getCell()).orElseThrow();
        cell.setStatus(RackCell.Status.OCCUPIED);
        cells.save(cell);
        movements.save(new StockMovement(cell.getProductId(), cell.getId(), null, StockMovement.Type.IN, cell.getQuantity(), job.getUsername()));
        announce("Box stored in cell " + job.getCell() + ": " + job.getLabel());
    }

    void bookShippedBox(WarehouseJob job) {
        RackCell cell = cells.findById(job.getCell()).orElseThrow();
        movements.save(new StockMovement(cell.getProductId(), cell.getId(), job.getOrderId(), StockMovement.Type.OUT, cell.getQuantity(), job.getUsername()));
        cell.empty();
        cells.save(cell);
        if (job.getOrderId() != null) orders.recordShippedBox(job.getOrderId());
        announce("Box shipped from cell " + job.getCell() + (job.getOrderId() == null ? "" : " for order #" + job.getOrderId()));
    }

    public boolean isBooking() { return pendingBookings.get() > 0; }

    public List<RackCell> cells() { return cells.findAll(); }

    public Optional<RackCell> oldestBoxOf(int productId) { return cells.oldestBoxOf(productId); }

    public List<StockMovement> latestMovements(int limit) { return movements.latest(limit); }

    public int freeCells() { return cells.countEmpty(); }

    public void onChange(Runnable listener) { changeListeners.add(listener); }

    public void removeOnChange(Runnable listener) { changeListeners.remove(listener); }

    private void notifyChanged() { changeListeners.forEach(Runnable::run); }

    public void onMessage(Consumer<String> listener) { messageListeners.add(listener); }

    public void removeOnMessage(Consumer<String> listener) { messageListeners.remove(listener); }

    private void announce(String message) { messageListeners.forEach(l -> l.accept(message)); }

    public void connect() {
        controller.connect(new ModbusTcpDriver(settings.factoryIoHost, settings.factoryIoPort, settings.slaveId), settings.craneConfig());
    }

    public void disconnect() { controller.disconnect(); }

    public AppSettings settings() { return settings; }

    public void saveSettings(AppSettings newSettings) {
        session.require(Permission.CONFIGURE_SYSTEM);
        if (newSettings.factoryIoHost.isBlank() || newSettings.factoryIoPort < 1 || newSettings.factoryIoPort > 65535)
            throw new BusinessException("Invalid Factory IO address");
        newSettings.save(settingsFile);
        settings = newSettings;
        controller.setConfig(newSettings.craneConfig());
    }
}
