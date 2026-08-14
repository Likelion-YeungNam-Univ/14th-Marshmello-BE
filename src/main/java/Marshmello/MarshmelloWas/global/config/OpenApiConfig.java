package Marshmello.MarshmelloWas.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import jakarta.annotation.PostConstruct;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CsrfToken;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @PostConstruct
    void ignoreCsrfTokenRequestWrapper() {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(CsrfToken.class);
    }

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Marshmello WAS API")
                .version("v1")
                .description("Runtime API documentation for Marshmello WAS."));
    }
}
