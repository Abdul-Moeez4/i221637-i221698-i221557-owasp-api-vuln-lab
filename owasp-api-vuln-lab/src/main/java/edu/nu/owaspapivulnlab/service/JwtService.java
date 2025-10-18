package edu.nu.owaspapivulnlab.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.ttl-seconds}")
    private long ttlSeconds;
    
    @Value("${app.jwt.issuer}")
    private String issuer;
    
    @Value("${app.jwt.audience}")
    private String audience;

    // Q7 FIX: Hardened JWT generation with proper key, short TTL, issuer/audience
    public String issue(String subject, Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());  // Q7 FIX: Proper key generation
        
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuer(issuer)                              // Q7 FIX: Set issuer
                .setAudience(audience)                          // Q7 FIX: Set audience
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlSeconds * 1000))  // Q7 FIX: Short TTL (1 hour)
                .signWith(key)                                  // Q7 FIX: Use proper signing method
                .compact();
    }
}
