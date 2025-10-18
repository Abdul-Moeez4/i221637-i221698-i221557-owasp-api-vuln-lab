package edu.nu.owaspapivulnlab.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;   // <-- added
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.*;

import java.io.IOException;
import java.util.Collections;

@Configuration
public class SecurityConfig {

    @Value("${app.jwt.secret}")
    private String secret;
    
    @Value("${app.jwt.ttl-seconds}")
    private long ttlSeconds;
    
    @Value("${app.jwt.issuer}")
    private String issuer;
    
    @Value("${app.jwt.audience}")
    private String audience;

    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // FIXED: Secure access control configuration (Q2 fix)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ---------- Q2 CHANGES ----------
            .authorizeHttpRequests(reg -> reg
                .requestMatchers("/api/auth/**", "/h2-console/**").permitAll() // public: login + H2 console
                // REMOVED (vulnerable): .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")              // RBAC for admin endpoints
                .anyRequest().authenticated()                                   // everything else requires auth
            )
            // --------------------------------

            .headers(h -> h.frameOptions(f -> f.disable())); // allow H2 console in frame

        http.addFilterBefore(
            new JwtFilter(secret, issuer, audience),  // Q7 FIX: Pass issuer and audience for validation
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
    
    // Q7 FIX: Hardened JWT filter with proper validation
    static class JwtFilter extends OncePerRequestFilter {
        private final String secret;
        private final String issuer;
        private final String audience;
        
        JwtFilter(String secret, String issuer, String audience) { 
            this.secret = secret; 
            this.issuer = issuer;
            this.audience = audience;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                try {
                    // Q7 FIX: Strict JWT validation with issuer, audience, and expiry checks
                    Claims c = Jwts.parserBuilder()
                            .setSigningKey(secret.getBytes())
                            .requireIssuer(issuer)           // Validate issuer
                            .requireAudience(audience)       // Validate audience
                            .build()
                            .parseClaimsJws(token)
                            .getBody();
                    
                    String user = c.getSubject();
                    String role = (String) c.get("role");
                    
                    UsernamePasswordAuthenticationToken authn = new UsernamePasswordAuthenticationToken(
                            user, null,
                            role != null
                                    ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                                    : Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authn);
                } catch (JwtException e) {
                    // Q7 FIX: Log security events but don't expose details to client
                    System.err.println("JWT validation failed: " + e.getClass().getSimpleName());
                    // Continue as anonymous (no authentication set)
                }
            }
            chain.doFilter(request, response);
        }
    }
}