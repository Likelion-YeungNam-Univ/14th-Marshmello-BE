package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.FileSystemResource;

class DeploymentWorkflowPolicyTest {

    @Test
    void ciRunsForEveryPushedBranchWithoutCallingDeployment() throws IOException {
        String ci = workflow(".github/workflows/ci.yml");

        assertThat(ci)
                .contains("on:\n  push:\n")
                .contains("./gradlew clean test --no-daemon")
                .contains("docker build")
                .contains("docker compose")
                .doesNotContain("branches:")
                .doesNotContain("branches-ignore:")
                .doesNotContain("uses: ./.github/workflows/deploy.yml")
                .doesNotContain("\n  deploy:\n");
    }

    @Test
    void deploymentRunsOnlyForMasterPushes() throws IOException {
        String deploy = workflow(".github/workflows/deploy.yml");

        assertThat(deploy)
                .contains("on:\n  push:\n    branches:\n      - master\n")
                .contains("APP_CORS_ALLOWED_ORIGINS: ${{ vars.APP_CORS_ALLOWED_ORIGINS }}")
                .contains("APP_LOGIN_SUCCESS_URL: ${{ vars.APP_LOGIN_SUCCESS_URL }}")
                .doesNotContain("workflow_call:")
                .doesNotContain("refs/heads/develop")
                .doesNotContain("refs/heads/main")
                .doesNotContain("|| 'http://localhost:5173'");
    }

    @Test
    void composePublishesOnlyTheApplicationPortOnLoopback() throws IOException {
        String compose = workflow("compose.yaml");

        assertThat(compose)
                .contains("target: 8080")
                .contains("host_ip: 127.0.0.1")
                .contains("APP_CORS_ALLOWED_ORIGINS: ${APP_CORS_ALLOWED_ORIGINS:?APP_CORS_ALLOWED_ORIGINS is required}")
                .contains("APP_LOGIN_SUCCESS_URL: ${APP_LOGIN_SUCCESS_URL:?APP_LOGIN_SUCCESS_URL is required}")
                .doesNotContain("0.0.0.0")
                .doesNotContain("5174")
                .doesNotContain("http://localhost:5173");
    }

    @Test
    void workflowFilesAreValidYaml() {
        for (String path : List.of(
                ".github/workflows/ci.yml",
                ".github/workflows/deploy.yml",
                "compose.yaml")) {
            YamlMapFactoryBean yaml = new YamlMapFactoryBean();
            yaml.setResources(new FileSystemResource(path));

            assertThat(yaml.getObject()).as(path).isNotEmpty();
        }
    }

    private static String workflow(String path) throws IOException {
        return Files.readString(Path.of(path)).replace("\r\n", "\n");
    }
}
