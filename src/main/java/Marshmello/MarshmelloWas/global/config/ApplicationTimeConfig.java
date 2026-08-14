package Marshmello.MarshmelloWas.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationTimeConfig {

    @Bean
    Clock applicationClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
