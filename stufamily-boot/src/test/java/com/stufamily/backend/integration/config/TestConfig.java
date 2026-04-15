package com.stufamily.backend.integration.config;

import com.stufamily.backend.shared.security.ReplayGuardFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public ReplayGuardFilter replayGuardFilter() {
        return new ReplayGuardFilter() {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                            jakarta.servlet.http.HttpServletResponse response,
                                            jakarta.servlet.FilterChain filterChain)
                    throws jakarta.servlet.ServletException, java.io.IOException {
                filterChain.doFilter(request, response);
            }
        };
    }
}
