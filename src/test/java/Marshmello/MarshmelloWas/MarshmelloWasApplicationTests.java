package Marshmello.MarshmelloWas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

@SpringBootTest
class MarshmelloWasApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void resolvesSessionCookieEnvironmentVariables() throws Exception {
		Properties applicationProperties = PropertiesLoaderUtils.loadProperties(
				new FileSystemResource("src/main/resources/application.properties"));
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new MapPropertySource(
				"sessionCookieEnvironment",
				Map.of(
						"SESSION_COOKIE_SAME_SITE", "none",
						"SESSION_COOKIE_SECURE", "true")));
		PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);

		assertThat(resolver.resolveRequiredPlaceholders(
				applicationProperties.getProperty("server.servlet.session.cookie.same-site"))).isEqualTo("none");
		assertThat(resolver.resolveRequiredPlaceholders(
				applicationProperties.getProperty("server.servlet.session.cookie.secure"))).isEqualTo("true");
	}

}
