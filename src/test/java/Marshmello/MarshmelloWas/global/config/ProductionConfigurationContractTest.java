package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;

class ProductionConfigurationContractTest {

    @Test
    void testRuntimeSuppliesExplicitDeterministicSecurityAndDatasourceValues() throws IOException {
        Properties properties = loadClasspathProperties("application.properties");

        assertThat(properties)
                .containsEntry("spring.datasource.url",
                        "jdbc:h2:mem:${random.uuid};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                .containsEntry("spring.datasource.username", "sa")
                .containsEntry("spring.security.oauth2.client.registration.oidc.client-id", "test-client")
                .containsEntry("spring.security.oauth2.client.registration.oidc.client-secret", "test-secret")
                .containsEntry("app.cors.allowed-origins", "http://localhost:5173")
                .containsEntry("app.login-success-url", "http://localhost:5173");
    }

    @Test
    void baseRuntimeRequiresDatasourceOidcCorsAndLoginInputsWithoutDefaults() throws IOException {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertThat(properties)
                .contains("spring.datasource.url=${SPRING_DATASOURCE_URL}")
                .contains("spring.datasource.username=${SPRING_DATASOURCE_USERNAME}")
                .contains("spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}")
                .contains("spring.security.oauth2.client.registration.oidc.client-id=${OIDC_CLIENT_ID}")
                .contains("spring.security.oauth2.client.registration.oidc.client-secret=${OIDC_CLIENT_SECRET}")
                .contains("spring.security.oauth2.client.provider.oidc.issuer-uri=${OIDC_ISSUER_URI}")
                .contains("app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS}")
                .contains("app.login-success-url=${APP_LOGIN_SUCCESS_URL}")
                .doesNotContain("${APP_CORS_ALLOWED_ORIGINS:")
                .doesNotContain("${SPRING_DATASOURCE_URL:")
                .doesNotContain("${OIDC_CLIENT_ID:")
                .doesNotContain("${OIDC_CLIENT_SECRET:")
                .doesNotContain("${OIDC_ISSUER_URI:");
    }

    @Test
    void springResolutionFailsWhenAnyRequiredBaseInputIsMissing() throws IOException {
        Properties application = loadFileProperties(Path.of("src/main/resources/application.properties"));
        Map<String, Object> fixture = new LinkedHashMap<>();
        fixture.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/marshmello");
        fixture.put("SPRING_DATASOURCE_USERNAME", "marshmello_app");
        fixture.put("SPRING_DATASOURCE_PASSWORD", "fixture-password-483921");
        fixture.put("OIDC_CLIENT_ID", "prod-client-483921");
        fixture.put("OIDC_CLIENT_SECRET", "fixture-client-secret-483921");
        fixture.put("OIDC_ISSUER_URI", "https://identity.acme-corp.com/oauth2/default");
        fixture.put("APP_CORS_ALLOWED_ORIGINS", "https://app.acme-corp.com");
        fixture.put("APP_LOGIN_SUCCESS_URL", "https://app.acme-corp.com/signed-in");
        Map<String, String> required = Map.of(
                "spring.datasource.url", "SPRING_DATASOURCE_URL",
                "spring.datasource.username", "SPRING_DATASOURCE_USERNAME",
                "spring.datasource.password", "SPRING_DATASOURCE_PASSWORD",
                "spring.security.oauth2.client.registration.oidc.client-id", "OIDC_CLIENT_ID",
                "spring.security.oauth2.client.registration.oidc.client-secret", "OIDC_CLIENT_SECRET",
                "spring.security.oauth2.client.provider.oidc.issuer-uri", "OIDC_ISSUER_URI",
                "app.cors.allowed-origins", "APP_CORS_ALLOWED_ORIGINS",
                "app.login-success-url", "APP_LOGIN_SUCCESS_URL");

        PropertySourcesPropertyResolver complete = resolver(application, fixture);
        required.forEach((property, environment) ->
                assertThat(complete.getRequiredProperty(property)).isEqualTo(fixture.get(environment)));

        required.forEach((property, environment) -> {
            Map<String, Object> incomplete = new LinkedHashMap<>(fixture);
            incomplete.remove(environment);
            assertThatThrownBy(() -> resolver(application, incomplete).getRequiredProperty(property))
                    .as("missing %s", environment)
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void composeRequiresProductionInputsAndFixesDatasourceToServiceNetworking() throws IOException {
        String compose = Files.readString(Path.of("compose.yaml"));

        assertThat(compose)
                .contains("SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:?POSTGRES_DB is required}")
                .contains("SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:?POSTGRES_USER is required}")
                .contains("OIDC_ISSUER_URI: ${OIDC_ISSUER_URI:?OIDC_ISSUER_URI is required}")
                .contains("OIDC_CLIENT_ID: ${OIDC_CLIENT_ID:?OIDC_CLIENT_ID is required}")
                .contains("OIDC_CLIENT_SECRET: ${OIDC_CLIENT_SECRET:?OIDC_CLIENT_SECRET is required}")
                .contains("APP_CORS_ALLOWED_ORIGINS: ${APP_CORS_ALLOWED_ORIGINS:?APP_CORS_ALLOWED_ORIGINS is required}")
                .contains("APP_LOGIN_SUCCESS_URL: ${APP_LOGIN_SUCCESS_URL:?APP_LOGIN_SUCCESS_URL is required}")
                .doesNotContain("jdbc:postgresql://localhost")
                .doesNotContain("APP_CORS_ALLOWED_ORIGINS:-")
                .doesNotContain("OIDC_ISSUER_URI:-");
    }

    @Test
    void workflowAndDeployScriptKeepEveryRequiredValueAcrossTheRemoteBoundary() throws IOException {
        String workflow = Files.readString(Path.of(".github/workflows/deploy.yml"));
        String script = Files.readString(Path.of("scripts/deploy-ec2.sh"));
        List<String> required = List.of(
                "APP_CORS_ALLOWED_ORIGINS",
                "APP_LOGIN_SUCCESS_URL",
                "POSTGRES_DB",
                "POSTGRES_USER",
                "OIDC_ISSUER_URI",
                "OIDC_CLIENT_ID",
                "OIDC_CLIENT_SECRET");

        for (String name : required) {
            assertThat(workflow)
                    .as("workflow environment for %s", name)
                    .contains(name + ": ${{ ");
            assertThat(workflow)
                    .as("workflow validation for %s", name)
                    .contains("validate_production_value " + name);
            assertThat(workflow)
                    .as("remote forwarding for %s", name)
                    .contains("append_remote_env " + name + " \"$" + name + "\"");
            assertThat(script)
                    .as("deploy ingestion for %s", name)
                    .contains(name + "=\"${" + name + ":-}\"");
            assertThat(script)
                    .as("deploy validation for %s", name)
                    .contains("validate_production_value " + name);
        }

        assertThat(script).contains(
                "export APP_CORS_ALLOWED_ORIGINS APP_LOGIN_SUCCESS_URL",
                "export POSTGRES_DB POSTGRES_USER",
                "export OIDC_ISSUER_URI OIDC_CLIENT_ID OIDC_CLIENT_SECRET");
        assertThat(workflow)
                .doesNotContain("APP_CORS_ALLOWED_ORIGINS: ${{ vars.APP_CORS_ALLOWED_ORIGINS ||")
                .doesNotContain("APP_LOGIN_SUCCESS_URL: ${{ vars.APP_LOGIN_SUCCESS_URL ||");
    }

    @Test
    void deploymentValidatorsRejectKnownPlaceholdersAndInvalidUrls() throws Exception {
        List<InvalidProductionValue> invalidValues = List.of(
                new InvalidProductionValue(
                        "OIDC_CLIENT_ID",
                        "replace-me.apps.googleusercontent.com",
                        "known repository client ID placeholder"),
                new InvalidProductionValue("OIDC_CLIENT_SECRET", "change-me", "known base placeholder"),
                new InvalidProductionValue("OIDC_CLIENT_SECRET", "replace-me", "known example placeholder"),
                new InvalidProductionValue(
                        "OIDC_ISSUER_URI",
                        "https://localhost.localdomain/oauth",
                        "explicit localhost host"),
                new InvalidProductionValue(
                        "OIDC_ISSUER_URI",
                        "https:///oauth",
                        "malformed issuer URL"),
                new InvalidProductionValue(
                        "APP_CORS_ALLOWED_ORIGINS",
                        "http://app.acme-corp.com",
                        "non-HTTPS CORS origin"));

        for (InvalidProductionValue invalid : invalidValues) {
            assertThat(runDeployValidator(invalid.name(), invalid.value()).exitCode())
                    .as(invalid.description())
                    .isNotZero();
        }

        assertThat(runDeployValidator("OIDC_CLIENT_SECRET", "client's-opaque-secret-483921").exitCode())
                .as("legitimate opaque credential punctuation")
                .isZero();
        assertThat(runDeployValidator("OIDC_CLIENT_ID", "exchange-mechanism-483921").exitCode())
                .as("lexical content adjacent to change/me letters")
                .isZero();
        assertThat(runDeployValidator("OIDC_CLIENT_SECRET", "replacement-token-483921").exitCode())
                .as("lexical replacement content")
                .isZero();
    }

    @Test
    void workflowAndDeployUseTheSameValidatorFunctions() throws IOException {
        String workflow = Files.readString(Path.of(".github/workflows/deploy.yml"));
        String script = Files.readString(Path.of("scripts/deploy-ec2.sh"));
        String workflowValidators = extractBetween(
                workflow,
                "          is_placeholder_value() {",
                "          [[ \"$RELEASE_ID\" =~");
        String deployValidators = extractBetween(script, "is_placeholder_value() {", "owner_uid() {");

        assertThat(stripIndent(workflowValidators, 10)).isEqualTo(deployValidators.strip());
    }

    @Test
    void embeddedWhitespaceOpaqueCredentialsPassBothValidatorsAndRemoteForwardingByteExact() throws Exception {
        String workflow = Files.readString(Path.of(".github/workflows/deploy.yml"));
        String script = Files.readString(Path.of("scripts/deploy-ec2.sh"));
        String workflowValidators = stripIndent(extractBetween(
                workflow,
                "          is_placeholder_value() {",
                "          [[ \"$RELEASE_ID\" =~"), 10);
        String deployValidators = extractBetween(script, "is_placeholder_value() {", "owner_uid() {");
        Map<String, String> credentials = Map.of(
                "OIDC_CLIENT_ID", "opaque client 483921",
                "OIDC_CLIENT_SECRET", "opaque secret 483921");

        assertThat(workflowValidators).isEqualTo(deployValidators);
        for (Map.Entry<String, String> credential : credentials.entrySet()) {
            assertThat(runDeployValidator(credential.getKey(), credential.getValue()).exitCode())
                    .as("identical workflow/deploy validator accepts embedded whitespace in opaque %s",
                            credential.getKey())
                    .isZero();
        }

        ValidationResult forwarding = runWorkflowForwarding(workflow, credentials);
        assertThat(forwarding.exitCode())
                .as("workflow forwarding preserves embedded whitespace credentials byte-exact")
                .isZero();
    }

    @Test
    void composeDeploymentTestSuppliesEveryRequiredNonSecretFixture() throws IOException {
        String compose = Files.readString(Path.of("compose.yaml"));
        String ci = Files.readString(Path.of(".github/workflows/ci.yml"));
        String step = extractBetween(ci, "      - name: Run Compose deployment test", "\n  deploy:");
        Map<String, String> environment = parseStepEnvironment(step);

        assertThat(environment)
                .containsEntry("OIDC_ISSUER_URI", "https://accounts.google.com")
                .containsEntry("APP_CORS_ALLOWED_ORIGINS", "https://app.example.test")
                .containsEntry("APP_LOGIN_SUCCESS_URL", "https://app.example.test/signed-in");
        assertThat(parseSuppliedEnvironment(step))
                .containsAll(parseRequiredEnvironment(compose));
    }

    private static Properties loadClasspathProperties(String resourceName) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = ProductionConfigurationContractTest.class
                .getClassLoader()
                .getResourceAsStream(resourceName)) {
            assertThat(input).as("classpath resource %s", resourceName).isNotNull();
            properties.load(input);
        }
        return properties;
    }

    private static Properties loadFileProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        return properties;
    }

    private static PropertySourcesPropertyResolver resolver(
            Properties application,
            Map<String, Object> environment
    ) {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("fixture-environment", environment));
        propertySources.addLast(new PropertiesPropertySource("application", application));
        return new PropertySourcesPropertyResolver(propertySources);
    }

    private static ValidationResult runDeployValidator(String name, String value) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                bashExecutable().toString(),
                "-c",
                "source <(sed '/^owner_uid()/,$d' scripts/deploy-ec2.sh); "
                        + "validate_production_value \"$VALIDATION_TARGET\"");
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("VALIDATION_TARGET", name);
        processBuilder.environment().put(name, value);
        Process process = processBuilder.start();
        String output;
        try (InputStream input = process.getInputStream()) {
            output = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        return new ValidationResult(process.waitFor(), output);
    }

    private static ValidationResult runWorkflowForwarding(
            String workflow,
            Map<String, String> credentials
    ) throws Exception {
        String forwardingFunction = stripIndent(extractBetween(
                workflow,
                "          append_remote_env() {",
                "          append_remote_env RELEASE_ID"), 10);
        String command = """
                set -euo pipefail
                remote_env=()
                %s
                expected_client_id=$OIDC_CLIENT_ID
                expected_client_secret=$OIDC_CLIENT_SECRET
                append_remote_env OIDC_CLIENT_ID "$OIDC_CLIENT_ID"
                append_remote_env OIDC_CLIENT_SECRET "$OIDC_CLIENT_SECRET"
                unset OIDC_CLIENT_ID OIDC_CLIENT_SECRET
                eval "${remote_env[0]}"
                eval "${remote_env[1]}"
                [[ "$OIDC_CLIENT_ID" == "$expected_client_id" ]]
                [[ "$OIDC_CLIENT_SECRET" == "$expected_client_secret" ]]
                """.formatted(forwardingFunction);
        ProcessBuilder processBuilder = new ProcessBuilder(bashExecutable().toString());
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("OIDC_CLIENT_ID", credentials.get("OIDC_CLIENT_ID"));
        processBuilder.environment().put("OIDC_CLIENT_SECRET", credentials.get("OIDC_CLIENT_SECRET"));
        return run(processBuilder, command);
    }

    private static ValidationResult run(ProcessBuilder processBuilder, String standardInput) throws Exception {
        Process process = processBuilder.start();
        try (var output = process.getOutputStream()) {
            output.write(standardInput.getBytes(StandardCharsets.UTF_8));
        }
        String output;
        try (InputStream input = process.getInputStream()) {
            output = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        return new ValidationResult(process.waitFor(), output);
    }

    private static Map<String, String> parseStepEnvironment(String step) {
        Map<String, String> environment = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("(?m)^ {10}([A-Z][A-Z0-9_]*): (.+)$").matcher(step);
        while (matcher.find()) {
            environment.put(matcher.group(1), matcher.group(2));
        }
        return environment;
    }

    private static Set<String> parseSuppliedEnvironment(String step) {
        Set<String> supplied = new LinkedHashSet<>(parseStepEnvironment(step).keySet());
        Matcher matcher = Pattern.compile("(?m)^\\s*export ([A-Z][A-Z0-9_]*)=").matcher(step);
        while (matcher.find()) {
            supplied.add(matcher.group(1));
        }
        return supplied;
    }

    private static Set<String> parseRequiredEnvironment(String compose) {
        Set<String> required = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*):\\?").matcher(compose);
        while (matcher.find()) {
            required.add(matcher.group(1));
        }
        return required;
    }

    private static Path bashExecutable() {
        String configured = System.getenv("OMO_CODEX_GIT_BASH_PATH");
        List<Path> candidates = List.of(
                configured == null ? Path.of("__not_configured__") : Path.of(configured),
                Path.of("C:/Program Files/Git/bin/bash.exe"),
                Path.of("/bin/bash"));
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Bash is required for deployment contract tests"));
    }

    private static String extractBetween(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        int endIndex = text.indexOf(end, startIndex);
        assertThat(startIndex).as("validator block start").isGreaterThanOrEqualTo(0);
        assertThat(endIndex).as("validator block end").isGreaterThan(startIndex);
        return text.substring(startIndex, endIndex).replace("\r\n", "\n").strip();
    }

    private static String stripIndent(String text, int spaces) {
        String prefix = " ".repeat(spaces);
        return text.lines()
                .map(line -> line.startsWith(prefix) ? line.substring(spaces) : line)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("")
                .strip();
    }

    private record InvalidProductionValue(String name, String value, String description) {
    }

    private record ValidationResult(int exitCode, String output) {
    }
}
