package com.stufamily.backend.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);
    private static final String TRACE_ID = "traceId";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String traceId = Optional.ofNullable(request.getHeader(HEADER_TRACE_ID)).orElse(UUID.randomUUID().toString());
        long start = System.currentTimeMillis();
        MDC.put(TRACE_ID, traceId);
        response.setHeader(HEADER_TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            log.info("request path={} method={} status={} cost={}ms", request.getRequestURI(), request.getMethod(),
                response.getStatus(), cost);
            MDC.remove(TRACE_ID);
        }
    }
}

