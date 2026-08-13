package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class UserProfileCompletedMigrationTest {

    @Test
    void keepsExistingProfilesCompletedAndDefaultsNewProfilesToPending() throws Exception {
        String url = "jdbc:h2:mem:migration-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V1__init_erd.sql"));
            statement.executeUpdate(
                    "INSERT INTO users (nickname) VALUES ('기존사용자')");
        }

        Flyway.configure()
                .dataSource(url, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("1"))
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            assertThat(readProfileCompleted(connection, "기존사용자")).isTrue();

            statement.executeUpdate(
                    "INSERT INTO users (nickname) VALUES ('신규사용자')");
            assertThat(readProfileCompleted(connection, "신규사용자")).isFalse();
        }
    }

    private static boolean readProfileCompleted(Connection connection, String nickname)
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT profile_completed FROM users WHERE nickname = ?")) {
            statement.setString(1, nickname);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }
}
