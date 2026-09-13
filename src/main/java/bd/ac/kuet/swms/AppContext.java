package bd.ac.kuet.swms;

import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.net.ExchangeRateService;
import bd.ac.kuet.swms.net.HttpJsonClient;
import bd.ac.kuet.swms.repository.CategoryRepository;
import bd.ac.kuet.swms.repository.ProductRepository;
import bd.ac.kuet.swms.repository.RackCellRepository;
import bd.ac.kuet.swms.repository.SalesOrderRepository;
import bd.ac.kuet.swms.repository.StockMovementRepository;
import bd.ac.kuet.swms.repository.TransactionRepository;
import bd.ac.kuet.swms.repository.UserRepository;
import bd.ac.kuet.swms.service.AuthService;
import bd.ac.kuet.swms.service.FinanceService;
import bd.ac.kuet.swms.service.ProductService;
import bd.ac.kuet.swms.service.SalesOrderService;
import bd.ac.kuet.swms.service.Session;
import bd.ac.kuet.swms.service.WarehouseAutomation;
import bd.ac.kuet.swms.service.WarehouseService;

import java.nio.file.Path;

public final class AppContext {
    private static AppContext current;

    public final Database database;
    public final Session session = new Session();
    public final AuthService auth;
    public final ProductService products;
    public final SalesOrderService orders;
    public final WarehouseService warehouse;
    public final WarehouseAutomation automation;
    public final FinanceService finance;
    public final ExchangeRateService exchangeRates;

    private AppContext(Path dataFolder) {
        database = new Database(dataFolder.resolve("warehouse.db"));
        ProductRepository productRepository = new ProductRepository(database);
        TransactionRepository transactionRepository = new TransactionRepository(database);

        auth = new AuthService(new UserRepository(database), session);
        auth.createDefaultUsersIfNone();
        products = new ProductService(productRepository, new CategoryRepository(database), session);
        orders = new SalesOrderService(new SalesOrderRepository(database), session);
        warehouse = new WarehouseService(database, new RackCellRepository(database), productRepository,
                new StockMovementRepository(database), transactionRepository, orders, session, dataFolder.resolve("settings.json"));
        automation = new WarehouseAutomation(warehouse, orders, session);
        finance = new FinanceService(transactionRepository, productRepository, session);

        HttpJsonClient http = new HttpJsonClient();
        exchangeRates = new ExchangeRateService(http);
    }

    public static AppContext start(Path dataFolder) {
        current = new AppContext(dataFolder);
        return current;
    }

    public static AppContext get() { return current; }
}
