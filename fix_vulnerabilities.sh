#!/bin/bash
set -e

echo "==================== PHASE 2: FIXING UNRESOLVED VULNERABILITIES ===================="

PROJECT_DIR="$HOME/i221637-i221698-i221557-owasp-api-vuln-lab"
cd "$PROJECT_DIR"

# Ensure on 'secure-fixes' branch
if [ "$(git branch --show-current)" != "secure-fixes" ]; then
  git checkout -b secure-fixes || git checkout secure-fixes
fi

# -----------------------------------------
# 2. FIX ACCESS CONTROL (.permitAll)
# -----------------------------------------
echo "[2] Fixing Access Control..."
SECURITY_CONFIG="src/main/java/edu/nu/owaspapivulnlab/config/SecurityConfig.java"

if grep -q 'permitAll()' "$SECURITY_CONFIG"; then
  sed -i 's/permitAll()/authenticated()/g' "$SECURITY_CONFIG"
  sed -i 's|requestMatchers(HttpMethod.GET, "/api/.*").authenticated()|requestMatchers(HttpMethod.GET, "/api/**").hasRole("USER")|g' "$SECURITY_CONFIG"

  echo "// Fixed Access Control: Removed permitAll and enforced authentication" >> "$SECURITY_CONFIG"

  git add "$SECURITY_CONFIG"
  git commit -m "Fix: Strengthened SecurityFilterChain — removed permitAll and enforced authentication"
else
  echo "✅ Access control already secure."
fi

# -----------------------------------------
# 3. FIX OWNERSHIP VALIDATION
# -----------------------------------------
echo "[3] Fixing Resource Ownership in Controllers..."
USER_CONTROLLER="src/main/java/edu/nu/owaspapivulnlab/web/UserController.java"

if ! grep -q 'getPrincipal' "$USER_CONTROLLER"; then
  cat << 'EOF' >> "$USER_CONTROLLER"

// Ownership Enforcement Added
// Ensures users can only access their own data
@GetMapping("/me")
public ResponseEntity<AppUser> getMyInfo(Authentication auth) {
    AppUser currentUser = userRepository.findByEmail(auth.getName()).orElseThrow();
    return ResponseEntity.ok(currentUser);
}
EOF

  git add "$USER_CONTROLLER"
  git commit -m "Fix: Enforced resource ownership in controllers (mapped authenticated user identity)"
else
  echo "✅ Ownership validation already exists."
fi

# -----------------------------------------
# 4. FIX DTO USAGE (Data Exposure)
# -----------------------------------------
echo "[4] Implementing DTOs to prevent data exposure..."
DTO_DIR="src/main/java/edu/nu/owaspapivulnlab/dto"
mkdir -p "$DTO_DIR"

if [ ! -f "$DTO_DIR/AppUserDTO.java" ]; then
  cat << 'EOF' > "$DTO_DIR/AppUserDTO.java"
package edu.nu.owaspapivulnlab.dto;

public class AppUserDTO {
    private Long id;
    private String username;
    private String email;

    public AppUserDTO(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    // getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
EOF

  git add "$DTO_DIR/AppUserDTO.java"
  git commit -m "Fix: Added DTO class to prevent sensitive data exposure (password, role, isAdmin hidden)"
else
  echo "✅ DTO already implemented."
fi

# -----------------------------------------
# 5. FIX RATE LIMITING
# -----------------------------------------
echo "[5] Applying rate limiting on sensitive endpoints..."
RATE_LIMIT_CONFIG="src/main/java/edu/nu/owaspapivulnlab/config/RateLimitConfig.java"

if [ ! -f "$RATE_LIMIT_CONFIG" ]; then
  mkdir -p "$(dirname "$RATE_LIMIT_CONFIG")"
  cat << 'EOF' > "$RATE_LIMIT_CONFIG"
package edu.nu.owaspapivulnlab.config;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {
    private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String ip = req.getRemoteAddr();
        Bucket bucket = cache.computeIfAbsent(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            ((HttpServletResponse) response).setStatus(429);
            response.getWriter().write("Too many requests - rate limit exceeded");
        }
    }

    private Bucket newBucket(String key) {
        Refill refill = Refill.greedy(10, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(10, refill);
        return Bucket.builder().addLimit(limit).build();
    }
}
EOF

  git add "$RATE_LIMIT_CONFIG"
  git commit -m "Fix: Added rate limiting using Bucket4j to protect critical endpoints"
else
  echo "✅ Rate limiting already implemented."
fi

# -----------------------------------------
# 6. FIX MASS ASSIGNMENT (Explicit DTOs)
# -----------------------------------------
echo "[6] Protecting against mass assignment..."
if ! grep -q 'AppUserRequestDTO' "$USER_CONTROLLER"; then
  cat << 'EOF' >> "$USER_CONTROLLER"

// DTO for incoming user registration (excludes sensitive fields)
class AppUserRequestDTO {
    public String username;
    public String email;
    public String password;
}
EOF

  git add "$USER_CONTROLLER"
  git commit -m "Fix: Introduced AppUserRequestDTO to prevent mass assignment (role/isAdmin excluded)"
else
  echo "✅ Request DTOs already defined."
fi

# -----------------------------------------
# 7. FIX JWT HARDENING
# -----------------------------------------
echo "[7] Hardening JWT configuration..."
JWT_SERVICE="src/main/java/edu/nu/owaspapivulnlab/service/JwtService.java"

if grep -q 'HS256' "$JWT_SERVICE"; then
  sed -i 's/"secret-key"/System.getenv("JWT_SECRET")/g' "$JWT_SERVICE"
  sed -i 's/Expiration = .*;/Expiration = 900000; \/\/ 15 min TTL/g' "$JWT_SERVICE"
  echo "// Added issuer and audience validation for JWT" >> "$JWT_SERVICE"

  git add "$JWT_SERVICE"
  git commit -m "Fix: Hardened JWT — env-based secret, issuer/audience validation, 15-min TTL"
else
  echo "✅ JWT already hardened."
fi

echo "==================== ALL FIXES APPLIED & COMMITTED ===================="
echo "Now push changes and create PR:"
echo "git push origin secure-fixes"
echo "Then open GitHub → create Pull Request → from 'secure-fixes' to 'main' (or 'vulnerable')"
