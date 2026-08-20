package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

class CareActionSeedRecoveryMigrationTest {

    @Test
    void keepsExistingActionsWhenTheRecoveryMigrationRuns() throws Exception {
        String url = "jdbc:h2:mem:care-action-existing-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

        migrateThroughVersionSix(url);
        migrateToLatest(url);

        assertThat(countActions(url)).isEqualTo(84);
    }

    @Test
    void restoresMissingActionsAfterTheOriginalSeedWasAlreadyRecorded() throws Exception {
        String url = "jdbc:h2:mem:care-action-recovery-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

        migrateThroughVersionSix(url);
        assertThat(countActions(url)).isEqualTo(84);

        deleteActions(url);
        assertThat(countActions(url)).isZero();

        migrateToLatest(url);
        migrateToLatest(url);

        assertThat(countActions(url)).isEqualTo(84);
    }

    private static void migrateThroughVersionSix(String url) {
        Flyway.configure()
                .dataSource(url, "sa", "")
                .target(MigrationVersion.fromVersion("6"))
                .load()
                .migrate();
    }

    private static void migrateToLatest(String url) {
        Flyway.configure()
                .dataSource(url, "sa", "")
                .load()
                .migrate();
    }

    private static int countActions(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM actions")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static void deleteActions(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM actions");
        }
    }
}
