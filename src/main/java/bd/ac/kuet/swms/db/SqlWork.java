package bd.ac.kuet.swms.db;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlWork<R> {
    R run(Connection connection) throws SQLException;
}
