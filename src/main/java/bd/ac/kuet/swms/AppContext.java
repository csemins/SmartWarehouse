package bd.ac.kuet.swms;

import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.repository.CategoryRepository;
import bd.ac.kuet.swms.repository.ProductRepository;
import bd.ac.kuet.swms.repository.UserRepository;
import bd.ac.kuet.swms.service.AuthService;
import bd.ac.kuet.swms.service.ProductService;
import bd.ac.kuet.swms.service.Session;

import java.nio.file.Path;

public final class AppContext {
    private static AppContext current;

    public final Database database;
    public final Session session = new Session();
    public final AuthService auth;
    public final ProductService products;

    private AppContext(Path dataFolder) {
        database = new Database(dataFolder.resolve("warehouse.db"));
        ProductRepository productRepository = new ProductRepository(database);

        auth = new AuthService(new UserRepository(database), session);
        auth.createDefaultUsersIfNone();
        products = new ProductService(productRepository, new CategoryRepository(database), session);

    }

    public static AppContext start(Path dataFolder) {
        current = new AppContext(dataFolder);
        return current;
    }

    public static AppContext get() { return current; }
}
