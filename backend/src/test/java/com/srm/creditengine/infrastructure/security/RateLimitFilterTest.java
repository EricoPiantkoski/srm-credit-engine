package com.srm.creditengine.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter(2, Duration.ofMinutes(1));

    private HttpServletResponse doFilter(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void allowsRequestsWithinCapacity() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/liquidacoes");
        request.setRemoteAddr("192.168.0.1");

        assertThat(doFilter(request).getStatus()).isEqualTo(200);
        assertThat(doFilter(request).getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsWhenCapacityExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/liquidacoes");
        request.setRemoteAddr("192.168.0.2");

        doFilter(request);
        doFilter(request);

        HttpServletResponse third = doFilter(request);
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    void treatsClientsIndependently() throws Exception {
        MockHttpServletRequest a = new MockHttpServletRequest("POST", "/api/recebiveis");
        a.setRemoteAddr("10.0.0.1");
        MockHttpServletRequest b = new MockHttpServletRequest("POST", "/api/recebiveis");
        b.setRemoteAddr("10.0.0.2");

        doFilter(a);
        doFilter(a);
        assertThat(doFilter(a).getStatus()).isEqualTo(429);
        assertThat(doFilter(b).getStatus()).isEqualTo(200);
    }

    @Test
    void honorsXForwardedFor() throws Exception {
        MockHttpServletRequest a = new MockHttpServletRequest("POST", "/api/liquidacoes");
        a.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
        MockHttpServletRequest b = new MockHttpServletRequest("POST", "/api/liquidacoes");
        b.addHeader("X-Forwarded-For", "203.0.113.8, 10.0.0.1");

        doFilter(a);
        doFilter(a);
        assertThat(doFilter(a).getStatus()).isEqualTo(429);
        assertThat(doFilter(b).getStatus()).isEqualTo(200);
    }

    @Test
    void readRequestsAreNotLimited() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/liquidacoes/extrato");
        request.setRemoteAddr("192.168.0.3");

        for (int i = 0; i < 10; i++) {
            assertThat(doFilter(request).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void refreshAndLogoutAreNotLimited() throws Exception {
        MockHttpServletRequest refresh = new MockHttpServletRequest("POST", "/api/auth/refresh");
        refresh.setRemoteAddr("192.168.0.4");
        MockHttpServletRequest logout = new MockHttpServletRequest("POST", "/api/auth/logout");
        logout.setRemoteAddr("192.168.0.4");

        for (int i = 0; i < 10; i++) {
            assertThat(doFilter(refresh).getStatus()).isEqualTo(200);
            assertThat(doFilter(logout).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void chainInvokedOnPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/liquidacoes");
        request.setRemoteAddr("192.168.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void chainNotInvokedOnReject() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/liquidacoes");
        request.setRemoteAddr("192.168.0.6");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);
        filter.doFilter(request, response, chain);
        filter.doFilter(request, response, chain);

        verify(chain, times(2)).doFilter(request, response);
    }
}