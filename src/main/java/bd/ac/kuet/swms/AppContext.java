package bd.ac.kuet.swms;

import bd.ac.kuet.swms.db.Database;
import bd.ac.kuet.swms.repository.UserRepository;
import bd.ac.kuet.swms.service.AuthService;
import bd.ac.kuet.swms.service.Session;

import java.nio.file.Path;

public final class AppContext {
    private static AppContext current;

    public final Database database;
    public final Session session = new Session();
    public final AuthService auth;

    private AppContext(Path dataFolder) {
        database = new Database(dataFolder.resolve("warehouse.db"));

        auth = new AuthService(new UserRepository(database), session);
        auth.createDefaultUsersIfNone();

    }

    public static AppContext start(Path dataFolder) {
        current = new AppContext(dataFolder);
        return current;
    }

    public static AppContext get() { return current; }
}
