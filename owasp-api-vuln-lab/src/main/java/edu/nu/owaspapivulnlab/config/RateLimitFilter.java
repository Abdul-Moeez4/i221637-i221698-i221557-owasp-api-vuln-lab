package edu.nu.owaspapivulnlab.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class RateLimitFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // Simple placeholder filter logic
        chain.doFilter(request, response);
        ((HttpServletResponse) response).setHeader("X-RateLimit-Limit", "100");
    }
}
