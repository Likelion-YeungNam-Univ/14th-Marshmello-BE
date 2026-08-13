package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class V1SchemaBootstrapTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "USERS",
            "SOCIAL_ACCOUNT",
            "REPORT",
            "CHECK_IN",
            "IMAGES",
            "IMAGES_ANALYSIS",
            "BODY_DIARY",
            "ACTIONS",
            "CARE_CARD");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void initializesUnchangedV1SchemaInPostgreSqlMode() {
        List<String> tableNames = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'PUBLIC'",
                String.class);
        List<String> careCardColumns = jdbcTemplate.queryForList(
                "select column_name from information_schema.columns "
                        + "where table_schema = 'PUBLIC' and table_name = 'CARE_CARD'",
                String.class);

        assertThat(tableNames).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
        assertThat(careCardColumns).contains("ACTION_NAME", "ACTION_REASON", "SOURCE");
    }
}
