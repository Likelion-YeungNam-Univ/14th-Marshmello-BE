package db.migration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public final class V7__restore_care_actions_seed extends BaseJavaMigration {

    private static final String SEED_RESOURCE = "db/migration/V4__seed_care_actions.sql";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (hasAnyActions(connection)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(readSeedSql());
        }
    }

    private static boolean hasAnyActions(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM actions")) {
            resultSet.next();
            return resultSet.getInt(1) > 0;
        }
    }

    private static String readSeedSql() throws IOException {
        ClassLoader classLoader = V7__restore_care_actions_seed.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(SEED_RESOURCE)) {
            if (inputStream == null) {
                throw new IOException("Care action seed resource not found: " + SEED_RESOURCE);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
