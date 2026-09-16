package bd.ac.kuet.swms.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Database {
    // bump this whenever schema.sql changes, old dbs get rebuilt (v3 only needs image_url)
    public static final int SCHEMA_VERSION = 4;

    private final String url;

    public Database(Path file) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
        } catch (IOException e) {
            throw new DataAccessException("Cannot create folder for " + file, e);
        }
        url = "jdbc:sqlite:" + file.toAbsolutePath();
        createSchema();
    }

    public static Path dataFolder() {
        return Path.of(System.getProperty("user.home"), ".smart-warehouse");
    }

    public static Path defaultFile() {
        return dataFolder().resolve("warehouse.db");
    }

    public Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement st = c.createStatement()) {
            // sqlite has FKs OFF by default, per connection. lost an hour on this
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        }
        return c;
    }

    // used for booking stock + money together, either both or nothing
    public <R> R inTransaction(SqlWork<R> work) {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                R result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    public String getUrl() { return url; }

    private void createSchema() {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            int version = schemaVersion(st);
            if (version == 3) st.execute("ALTER TABLE products ADD COLUMN image_url TEXT");
            else if (version != SCHEMA_VERSION) dropAllTables(st);
            for (String sql : readSchemaStatements()) st.execute(sql);
            st.execute("PRAGMA user_version = " + SCHEMA_VERSION);
        } catch (IOException | SQLException e) {
            throw new DataAccessException("Could not create database schema: " + e.getMessage(), e);
        }
    }

    private static int schemaVersion(Statement st) throws SQLException {
        try (ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            return rs.getInt(1);
        }
    }

    private static void dropAllTables(Statement st) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")) {
            while (rs.next()) tables.add(rs.getString(1));
        }
        st.execute("PRAGMA foreign_keys = OFF");
        for (String t : tables) st.execute("DROP TABLE IF EXISTS \"" + t + "\"");
        st.execute("PRAGMA foreign_keys = ON");
    }

    private static List<String> readSchemaStatements() throws IOException {
        try (InputStream in = Database.class.getResourceAsStream("/bd/ac/kuet/swms/schema.sql")) {
            List<String> statements = new ArrayList<>();
            // naive split on ; - fine as long as schema.sql has no ; inside strings
            for (String part : new String(in.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
                if (!part.isBlank()) statements.add(part);
            }
            return statements;
        }
    }
}
