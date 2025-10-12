package edu.nu.owaspapivulnlab.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;     // <-- added
import org.springframework.web.bind.annotation.*;
import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.repo.AppUserRepository;
import edu.nu.owaspapivulnlab.service.JwtService;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserRepository users;
    private final JwtService jwt;
    private final PasswordEncoder passwordEncoder;               

    // Spring will inject PasswordEncoder because we defined the bean in SecurityConfig
    public AuthController(AppUserRepository users, JwtService jwt, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.jwt = jwt;
        this.passwordEncoder = passwordEncoder;                   
    }

    // ======== DTOs ========
    public static class LoginReq {
        @NotBlank private String username;
        @NotBlank private String password;
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public void setUsername(String username) { this.username = username; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class SignupReq {
        @NotBlank private String username;
        @NotBlank private String password;
        @NotBlank private String email;
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getEmail() { return email; }
        public void setUsername(String username) { this.username = username; }
        public void setPassword(String password) { this.password = password; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class TokenRes {
        private String token;
        public TokenRes() {}
        public TokenRes(String token) { this.token = token; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }

    // ======== LOGIN (uses BCrypt.matches) ========
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq req) {
        AppUser user = users.findByUsername(req.getUsername()).orElse(null);
        if (user != null && passwordEncoder.matches(req.getPassword(), user.getPassword())) { // <-- BCrypt check
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole());
            claims.put("isAdmin", user.isAdmin());
            String token = jwt.issue(user.getUsername(), claims);
            return ResponseEntity.ok(new TokenRes(token));
        }
        Map<String, String> error = new HashMap<>();
        error.put("error", "invalid credentials");
        return ResponseEntity.status(401).body(error);
    }

    // ======== SIGNUP (hash password before save) ========
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupReq req) {
        if (users.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "username already exists"));
        }
        AppUser u = AppUser.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))        // <-- hash here
                .email(req.getEmail())
                .role("USER")                                               // safe default
                .isAdmin(false)
                .build();
        users.save(u);

        // return minimal info (Q4 will replace with DTOs later)
        return ResponseEntity.created(URI.create("/api/users/" + u.getId()))
                .body(Map.of("id", u.getId(), "username", u.getUsername(), "email", u.getEmail()));
    }
}
