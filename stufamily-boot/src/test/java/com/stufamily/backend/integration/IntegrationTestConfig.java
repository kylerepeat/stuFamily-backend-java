package com.stufamily.backend.integration;

import com.stufamily.backend.shared.security.ReplayGuardFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class IntegrationTestConfig {

    @Bean
    @Primary
    public FilterRegistrationBean<ReplayGuardFilter> disableReplayGuardFilter(ReplayGuardFilter filter) {
        FilterRegistrationBean<ReplayGuardFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
