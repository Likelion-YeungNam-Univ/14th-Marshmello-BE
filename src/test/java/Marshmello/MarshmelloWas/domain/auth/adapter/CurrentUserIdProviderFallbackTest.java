package Marshmello.MarshmelloWas.domain.auth.adapter;

import Marshmello.MarshmelloWas.global.exception.AuthenticationRequiredException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserIdProviderFallbackTest {

    @Test
    void fallbackOnlyContextInjectsUnauthenticatedProvider() {
        try (AnnotationConfigApplicationContext context = contextFor(FallbackOnlyConfiguration.class)) {
            InjectedCurrentUser injectedCurrentUser = context.getBean(InjectedCurrentUser.class);

            assertThat(injectedCurrentUser.providerType())
                    .isEqualTo(UnauthenticatedCurrentUserIdProvider.class);
            assertThatThrownBy(injectedCurrentUser::requireCurrentUserId)
                    .isInstanceOf(AuthenticationRequiredException.class)
                    .hasNoCause();
        }
    }

    @Test
    void regularProviderWinsSingleValuedInjectionOverFallback() {
        try (AnnotationConfigApplicationContext context = contextFor(OneRegularProviderConfiguration.class)) {
            InjectedCurrentUser injectedCurrentUser = context.getBean(InjectedCurrentUser.class);

            assertThat(injectedCurrentUser.requireCurrentUserId()).isEqualTo(42L);
        }
    }

    @Test
    void twoRegularProvidersFailContextStartup() {
        assertThatThrownBy(() -> contextFor(TwoRegularProvidersConfiguration.class))
                .isInstanceOf(UnsatisfiedDependencyException.class)
                .hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class);
    }

    private static AnnotationConfigApplicationContext contextFor(Class<?> configurationClass) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(configurationClass);
        context.refresh();
        return context;
    }

    @Configuration(proxyBeanMethods = false)
    @Import(UnauthenticatedCurrentUserIdProvider.class)
    static class FallbackOnlyConfiguration {

        @Bean
        InjectedCurrentUser injectedCurrentUser(CurrentUserIdProvider currentUserIdProvider) {
            return new InjectedCurrentUser(currentUserIdProvider);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(UnauthenticatedCurrentUserIdProvider.class)
    static class OneRegularProviderConfiguration {

        @Bean
        CurrentUserIdProvider regularCurrentUserIdProvider() {
            return () -> 42L;
        }

        @Bean
        InjectedCurrentUser injectedCurrentUser(CurrentUserIdProvider currentUserIdProvider) {
            return new InjectedCurrentUser(currentUserIdProvider);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(UnauthenticatedCurrentUserIdProvider.class)
    static class TwoRegularProvidersConfiguration {

        @Bean
        CurrentUserIdProvider firstRegularCurrentUserIdProvider() {
            return () -> 1L;
        }

        @Bean
        CurrentUserIdProvider secondRegularCurrentUserIdProvider() {
            return () -> 2L;
        }

        @Bean
        InjectedCurrentUser injectedCurrentUser(CurrentUserIdProvider currentUserIdProvider) {
            return new InjectedCurrentUser(currentUserIdProvider);
        }
    }

    private record InjectedCurrentUser(CurrentUserIdProvider currentUserIdProvider) {

        Class<?> providerType() {
            return currentUserIdProvider.getClass();
        }

        long requireCurrentUserId() {
            return currentUserIdProvider.requireCurrentUserId();
        }
    }
}
