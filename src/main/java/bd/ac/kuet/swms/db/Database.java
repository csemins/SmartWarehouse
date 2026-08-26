package bd.ac.kuet.swms.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Database {

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
        return c;
    }

    public String getUrl() { return url; }

    private void createSchema() {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            for (String sql : readSchemaStatements()) st.execute(sql);
        } catch (IOException | SQLException e) {
            throw new DataAccessException("Could not create database schema: " + e.getMessage(), e);
        }
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
