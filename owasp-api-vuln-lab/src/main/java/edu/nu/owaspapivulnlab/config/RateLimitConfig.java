package edu.nu.owaspapivulnlab.config;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter();
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*"); // ✅ Apply to ALL paths instead of /api
        registration.setOrder(1);
        return registration;
    }

    // Inner class to keep logic encapsulated here
    public static class RateLimitFilter implements Filter {
        private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

        private Bucket createNewBucket() {
            Refill refill = Refill.greedy(10, Duration.ofMinutes(1)); // 10 requests/min
            Bandwidth limit = Bandwidth.classic(10, refill);
            return Bucket.builder().addLimit(limit).build();
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String ip = httpRequest.getRemoteAddr();

            Bucket bucket = buckets.computeIfAbsent(ip, k -> createNewBucket());

            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                httpResponse.setStatus(429);
                httpResponse.getWriter().write("Too Many Requests — Rate limit exceeded");
            }
        }
    }
}

