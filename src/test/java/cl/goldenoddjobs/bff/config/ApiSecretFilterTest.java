package cl.goldenoddjobs.bff.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ApiSecretFilterTest {

    private MockHttpServletRequest req(String method, String path, String secret) {
        MockHttpServletRequest r = new MockHttpServletRequest(method, path);
        if (secret != null) r.addHeader(ApiSecretFilter.HEADER, secret);
        return r;
    }

    @Test
    void noticeSinSecretoDa401() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new ApiSecretFilter("s3cr3t", false).doFilter(req("POST", "/api/notice", null), res, chain);
        assertEquals(401, res.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void noticeConSecretoIncorrectoDa401() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        new ApiSecretFilter("s3cr3t", false).doFilter(req("POST", "/api/notice", "otro"), res, mock(FilterChain.class));
        assertEquals(401, res.getStatus());
    }

    @Test
    void noticeConSecretoCorrectoPasa() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest r = req("POST", "/api/notice", "s3cr3t");
        new ApiSecretFilter("s3cr3t", false).doFilter(r, res, chain);
        verify(chain).doFilter(r, res);
    }

    @Test
    void otrasRutasNoPidenSecretoSalvoModoEstricto() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest r = req("GET", "/api/informes", null);
        new ApiSecretFilter("s3cr3t", false).doFilter(r, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(eq(r), any());

        MockHttpServletResponse res = new MockHttpServletResponse();
        new ApiSecretFilter("s3cr3t", true).doFilter(req("GET", "/api/informes", null), res, mock(FilterChain.class));
        assertEquals(401, res.getStatus());
    }

    @Test
    void healthYPreflightSiemprePasan() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        ApiSecretFilter f = new ApiSecretFilter("s3cr3t", true);
        f.doFilter(req("GET", "/api/health", null), new MockHttpServletResponse(), chain);
        f.doFilter(req("OPTIONS", "/api/notice", null), new MockHttpServletResponse(), chain);
        verify(chain, times(2)).doFilter(any(), any());
    }
}
