FROM eclipse-temurin:17-jdk-jammy@sha256:29467857e8bde40ab1f7befecbda0ea764b95afec1cc7f89aa90f7a766577e19 AS builder

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew \
    && ./gradlew clean test bootJar \
    && set -eux; \
       jar_count="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | wc -l)"; \
       test "$jar_count" -eq 1; \
       app_jar="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar')"; \
       mkdir -p /opt/app; \
       cp "$app_jar" /opt/app/app.jar

RUN mkdir -p /opt/probe \
    && printf '%s\n' \
        'import java.net.HttpURLConnection;' \
        'import java.net.URL;' \
        '' \
        'public final class Http404Probe {' \
        '    private Http404Probe() {}' \
        '' \
        '    public static void main(String[] args) {' \
        '        String target = args.length == 0 ? "http://127.0.0.1:8080/" : args[0];' \
        '        try {' \
        '            HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();' \
        '            connection.setConnectTimeout(2000);' \
        '            connection.setReadTimeout(2000);' \
        '            connection.setRequestMethod("GET");' \
        '            int status = connection.getResponseCode();' \
        '            connection.disconnect();' \
        '            if (status == 404) {' \
        '                System.exit(0);' \
        '            }' \
        '        } catch (Exception ignored) {' \
        '        }' \
        '        System.exit(1);' \
        '    }' \
        '}' > /opt/probe/Http404Probe.java \
    && javac --release 17 -d /opt/probe /opt/probe/Http404Probe.java \
    && rm /opt/probe/Http404Probe.java

FROM eclipse-temurin:17-jre-jammy@sha256:89e68b9bb83713510b63e2059a415792a7fc77e14b739a7d7ede97f6d9ca2c38 AS runtime

ARG BUILD_ID
LABEL BUILD_ID="${BUILD_ID}" \
      org.opencontainers.image.revision="${BUILD_ID}"

WORKDIR /app
COPY --from=builder --chown=10001:10001 /opt/app/app.jar /app/app.jar
COPY --from=builder --chown=10001:10001 /opt/probe/Http404Probe.class /app/healthcheck/Http404Probe.class

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=5s --timeout=3s --start-period=20s --retries=12 CMD ["java", "-cp", "/app/healthcheck", "Http404Probe", "http://127.0.0.1:8080/"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
