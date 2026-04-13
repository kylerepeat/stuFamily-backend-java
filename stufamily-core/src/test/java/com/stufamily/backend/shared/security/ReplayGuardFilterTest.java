package com.stufamily.backend.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ReplayGuardFilterTest {

    private final ReplayGuardFilter filter = new ReplayGuardFilter();

    @BeforeEach
    void beforeEach() {
        ReplayGuardFilter.clearForTest();
    }

    @Test
    void shouldBlockRepeatedGetWithin200ms() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest firstReq = new MockHttpServletRequest("GET", "/api/admin/orders");
        firstReq.setQueryString("page_no=1&page_size=20");
        firstReq.addHeader("Authorization", "Bearer test-get-token");
        MockHttpServletResponse firstResp = new MockHttpServletResponse();
        filter.doFilter(firstReq, firstResp, chain);
        verify(chain, times(1)).doFilter(firstReq, firstResp);

        MockHttpServletRequest secondReq = new MockHttpServletRequest("GET", "/api/admin/orders");
        secondReq.setQueryString("page_no=1&page_size=20");
        secondReq.addHeader("Authorization", "Bearer test-get-token");
        MockHttpServletResponse secondResp = new MockHttpServletResponse();
        filter.doFilter(secondReq, secondResp, chain);

        assertEquals(429, secondResp.getStatus());
        assertTrue(secondResp.getContentAsString().contains("TOO_MANY_REQUESTS"));
    }

    @Test
    void shouldBlockRepeatedPostWithin2s() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest firstReq = new MockHttpServletRequest("POST", "/api/admin/orders/ORD1/refund");
        firstReq.addHeader("Authorization", "Bearer test-post-token");
        MockHttpServletResponse firstResp = new MockHttpServletResponse();
        filter.doFilter(firstReq, firstResp, chain);

        MockHttpServletRequest secondReq = new MockHttpServletRequest("POST", "/api/admin/orders/ORD1/refund");
        secondReq.addHeader("Authorization", "Bearer test-post-token");
        MockHttpServletResponse secondResp = new MockHttpServletResponse();
        filter.doFilter(secondReq, secondResp, chain);

        assertEquals(429, secondResp.getStatus());
        assertTrue(secondResp.getContentAsString().contains("请求过于频繁"));
    }
}
