package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class ReportMonthMigrationTest {

    @Test
    void migratesExistingPeriodsAndEnforcesOneReportPerUserAndMonth() throws Exception {
        String url = "jdbc:h2:mem:report-month-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V1__init_erd.sql"));
            statement.executeUpdate("INSERT INTO users (nickname) VALUES ('report-owner')");
            statement.executeUpdate("""
                    INSERT INTO report (content, period_start, period_end, user_id)
                    VALUES ('기존 리포트', DATE '2026-07-03', DATE '2026-07-29', 1)
                    """);
        }

        Flyway.configure()
                .dataSource(url, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("1"))
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            assertThat(readReportMonth(connection)).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThatThrownBy(() -> insertReport(connection, LocalDate.of(2026, 7, 1)))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> insertReport(connection, LocalDate.of(2026, 7, 2)))
                    .isInstanceOf(SQLException.class);
        }
    }

    private static LocalDate readReportMonth(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT report_month FROM report")) {
            resultSet.next();
            return resultSet.getDate(1).toLocalDate();
        }
    }

    private static void insertReport(Connection connection, LocalDate reportMonth) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO report (content, report_month, user_id)
                VALUES ('새 리포트', ?, 1)
                """)) {
            statement.setDate(1, Date.valueOf(reportMonth));
            statement.executeUpdate();
        }
    }
}
